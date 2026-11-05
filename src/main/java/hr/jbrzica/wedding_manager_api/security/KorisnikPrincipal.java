package hr.jbrzica.wedding_manager_api.security;

import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;

// Prijavljeni korisnik, kako ga vidi Spring Security tijekom jednog zahtjeva.
public record KorisnikPrincipal(Long id, Long tvrtkaId, String email, UlogaKorisnika uloga) {
}
