package hr.jbrzica.wedding_manager_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Servisi preko ove klase saznaju tko je prijavljen i kojoj tvrtki pripada.
@Component
public class TrenutniKorisnik {

    public KorisnikPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof KorisnikPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niste prijavljeni");
        }
        return principal;
    }

    public Long id() {
        return get().id();
    }

    public Long tvrtkaId() {
        return get().tvrtkaId();
    }
}
