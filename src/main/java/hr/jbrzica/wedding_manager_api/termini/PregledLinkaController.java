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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Viber, WhatsApp i Facebook ne pokreću Angular: pročitaju samo HTML koji server vrati.
 * Zato za link rezervacije server sam upiše naslov, opis i sliku (Open Graph) u index.html.
 * Preglednik dobije istu aplikaciju kao i inače.
 */
@Controller
public class PregledLinkaController {

    // Kad Angular nije izgrađen (npr. backend testovi), dovoljna je osnovna stranica
    private static final String ZADANI_INDEX = """
            <!doctype html>
            <html lang="hr">
            <head>
              <meta charset="utf-8">
              <title>Wedding Manager</title>
            </head>
            <body><app-root></app-root></body>
            </html>
            """;

    private final JavniTerminiService javniService;
    private volatile String index;

    public PregledLinkaController(JavniTerminiService javniService) {
        this.javniService = javniService;
    }

    // Programi koji rade pregled linka u porukama; neki ne prate preusmjeravanje, pa njima kartica ide odmah
    private static final List<String> ROBOTI = List.of("whatsapp", "facebookexternalhit", "facebot", "viber",
            "telegrambot", "twitterbot", "slackbot", "discordbot", "linkedinbot", "skypeuripreview", "applebot",
            "pinterest", "snapchat", "redditbot", "embedly", "iframely");

    static boolean jeRobot(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return ROBOTI.stream().anyMatch(ua::contains);
    }

    @GetMapping(value = "/rezervacija/{kod}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> stranica(@PathVariable String kod) {
        return html(kod);
    }

    // Kratki link na samoj aplikaciji: app.brzicaweddings.com/r/bozicno
    @GetMapping("/r/{kratkiLink}")
    public ResponseEntity<String> kratkiLink(@PathVariable String kratkiLink,
                                             @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        Optional<String> kod = javniService.kodZaKratkiLink(kratkiLink.toLowerCase(Locale.ROOT));
        if (kod.isPresent() && jeRobot(userAgent)) {
            return html(kod.get());
        }
        String cilj = kod.map(k -> "/rezervacija/" + k).orElse("/");
        return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(cilj))
                .cacheControl(CacheControl.noStore()).build();
    }

    // Aplikacija s upisanim naslovom, opisom i slikom fotografiranja
    public ResponseEntity<String> html(String kod) {
        String html = index();
        String sadrzaj = javniService.pregled(kod).map(p -> sOznakama(html, p)).orElse(html);
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .body(sadrzaj);
    }

    // Mala stranica za poddomenu: kartica za Viber/WhatsApp, a preglednik odmah ide na cilj
    public String preusmjeravanje(String kod, String cilj) {
        String osnova = """
                <!doctype html>
                <html lang="hr">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta http-equiv="refresh" content="0;url=%1$s">
                  <title>Rezervacija</title>
                </head>
                <body>
                  <p><a href="%1$s">Otvori stranicu za rezervaciju</a></p>
                  <script>location.replace(%2$s);</script>
                </body>
                </html>
                """.formatted(e(cilj), jsString(cilj));
        return javniService.pregled(kod).map(p -> sOznakama(osnova, p)).orElse(osnova);
    }

    // Adresa kao JavaScript string; "<" se kodira da ne može zatvoriti <script>
    private static String jsString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c") + "\"";
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
                return ZADANI_INDEX;
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
