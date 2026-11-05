package hr.jbrzica.wedding_manager_api.security;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Za svaki zahtjev: ako ima zaglavlje "Authorization: Bearer <token>" s ispravnim tokenom,
 * korisnik se postavlja kao prijavljen. Ako nema ili ne valja, zahtjev ide dalje kao neprijavljen,
 * a SecurityConfig odluči smije li proći.
 */
public class JwtFilter extends OncePerRequestFilter {

    private final JwtServis jwtServis;
    private final KorisnikRepository korisnikRepository;

    public JwtFilter(JwtServis jwtServis, KorisnikRepository korisnikRepository) {
        this.jwtServis = jwtServis;
        this.korisnikRepository = korisnikRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String zaglavlje = request.getHeader("Authorization");

        if (zaglavlje != null && zaglavlje.startsWith("Bearer ")) {
            try {
                Long korisnikId = jwtServis.procitajKorisnikId(zaglavlje.substring(7));
                korisnikRepository.findById(korisnikId)
                        .filter(Korisnik::isEmailPotvrden)
                        .ifPresent(this::prijavi);
            } catch (JwtException | IllegalArgumentException e) {
                // neispravan token: korisnik ostaje neprijavljen
            }
        }

        filterChain.doFilter(request, response);
    }

    private void prijavi(Korisnik korisnik) {
        KorisnikPrincipal principal = new KorisnikPrincipal(
                korisnik.getId(), korisnik.getTvrtka().getId(), korisnik.getEmail(), korisnik.getUloga());
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + korisnik.getUloga().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
