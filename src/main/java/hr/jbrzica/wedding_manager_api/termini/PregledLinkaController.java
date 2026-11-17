package hr.jbrzica.wedding_manager_api.termini;

import hr.jbrzica.wedding_manager_api.termini.JavniTerminiService.PregledLinka;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Viber, WhatsApp i Facebook ne pokreću Angular: pročitaju samo HTML koji server vrati.
 * Zato za link rezervacije server sam upiše naslov, opis i sliku (Open Graph) u index.html.
 * Preglednik dobije istu aplikaciju kao i inače.
 */
@Controller
public class PregledLinkaController {

    private final JavniTerminiService javniService;
    private volatile String index;

    public PregledLinkaController(JavniTerminiService javniService) {
        this.javniService = javniService;
    }

    @GetMapping(value = "/rezervacija/{kod}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> stranica(@PathVariable String kod) {
        String html = index();
        String sadrzaj = javniService.pregled(kod).map(p -> sOznakama(html, p)).orElse(html);
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .body(sadrzaj);
    }

    static String sOznakama(String html, PregledLinka p) {
        StringBuilder oznake = new StringBuilder()
                .append("<title>").append(e(p.naslov())).append("</title>\n")
                .append(meta("og:type", "website"))
                .append(meta("og:title", p.naslov()))
                .append(meta("og:description", p.opis()))
                .append(meta("og:url", p.adresa()))
                .append("  <meta name=\"description\" content=\"").append(e(p.opis())).append("\">\n");
        if (p.slika() != null) {
            oznake.append(meta("og:image", p.slika()))
                    .append("  <meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        }
        // Zamijeni zadani naslov aplikacije; ako ga nema, upiši oznake prije kraja <head>
        String bezNaslova = html.replaceFirst("<title>[^<]*</title>", "");
        return bezNaslova.replaceFirst("</head>", java.util.regex.Matcher.quoteReplacement(oznake + "</head>"));
    }

    private static String meta(String svojstvo, String vrijednost) {
        return "  <meta property=\"" + svojstvo + "\" content=\"" + e(vrijednost) + "\">\n";
    }

    // Naziv i opis upisuje korisnik, pa se moraju escapeati (inače bi netko mogao ubaciti <script>)
    private static String e(String tekst) {
        return HtmlUtils.htmlEscape(tekst, StandardCharsets.UTF_8.name());
    }

    private String index() {
        String html = index;
        if (html == null) {
            ClassPathResource r = new ClassPathResource("static/index.html");
            if (!r.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            try (InputStream in = r.getInputStream()) {
                html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            index = html;
        }
        return html;
    }
}
