package hr.jbrzica.wedding_manager_api.termini;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Poddomena kao kratki link: bozicno.brzicaweddings.com vodi na rezervaciju fotografiranja s kratkim linkom "bozicno".
 * Poddomena mora u DNS-u pokazivati na ovaj server (Railway, custom domain), da vrijedi HTTPS certifikat.
 * Preusmjerava se uvijek na vlastitu javnu adresu iz postavki, nikad na adresu iz zahtjeva.
 */
@Component
public class KratkaDomenaFilter extends OncePerRequestFilter {

    private final DogadjajTerminaRepository dogadjajRepository;
    private final PregledLinkaController pregled;
    private final String javnaAdresa;
    private final String javniHost;
    private final String domena;

    public KratkaDomenaFilter(DogadjajTerminaRepository dogadjajRepository, PregledLinkaController pregled,
                              @Value("${app.javna-adresa}") String javnaAdresa,
                              @Value("${app.kratka-domena:}") String domena) {
        this.dogadjajRepository = dogadjajRepository;
        this.pregled = pregled;
        this.javnaAdresa = javnaAdresa.replaceAll("/+$", "");
        this.javniHost = URI.create(this.javnaAdresa).getHost();
        this.domena = domena.toLowerCase(Locale.ROOT).trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String oznaka = oznaka(request.getServerName());
        if (oznaka == null) {
            chain.doFilter(request, response);
            return;
        }
        java.util.Optional<String> kod = dogadjajRepository.findByKratkiLink(oznaka).map(DogadjajTermina::getJavniKod);
        // Viber/WhatsApp odmah dobiju karticu sa slikom; ljudi se preusmjere na stranicu za rezervaciju
        if (kod.isPresent() && PregledLinkaController.jeRobot(request.getHeader("User-Agent"))) {
            org.springframework.http.ResponseEntity<String> html = pregled.html(kod.get());
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.getWriter().write(html.getBody());
            return;
        }
        String cilj = kod.map(k -> javnaAdresa + "/rezervacija/" + k).orElse(javnaAdresa);
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(cilj);
    }

    // "bozicno" iz "bozicno.brzicaweddings.com"; null za sve ostale adrese (i za samu aplikaciju)
    String oznaka(String host) {
        if (domena.isEmpty() || host == null) {
            return null;
        }
        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals(javniHost) || !h.endsWith("." + domena)) {
            return null;
        }
        String oznaka = h.substring(0, h.length() - domena.length() - 1);
        return oznaka.isEmpty() || oznaka.contains(".") || oznaka.equals("www") ? null : oznaka;
    }
}
