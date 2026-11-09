package hr.jbrzica.wedding_manager_api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kolačić s refresh tokenom.
 * HttpOnly: JavaScript ga ne može pročitati, pa ga XSS ne može ukrasti.
 * SameSite=Strict: browser ga ne šalje sa zahtjevima s drugih stranica (zaštita od CSRF-a).
 * Path=/api/auth: šalje se samo na prijavu i osvježavanje, ne uz svaki zahtjev.
 */
@Component
public class RefreshKolacic {

    public static final String IME = "wm_osvjezi";

    private final boolean secure;

    public RefreshKolacic(@Value("${app.refresh.secure-kolacic}") boolean secure) {
        this.secure = secure;
    }

    public void postavi(HttpServletResponse response, String token, Duration trajanje) {
        response.addHeader(HttpHeaders.SET_COOKIE, izgradi(token, trajanje).toString());
    }

    public void obrisi(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, izgradi("", Duration.ZERO).toString());
    }

    private ResponseCookie izgradi(String vrijednost, Duration trajanje) {
        return ResponseCookie.from(IME, vrijednost)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(trajanje)
                .build();
    }
}
