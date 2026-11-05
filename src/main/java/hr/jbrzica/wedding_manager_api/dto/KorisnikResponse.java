package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;

public record KorisnikResponse(Long id, String ime, String email, UlogaKorisnika uloga,
                               Long tvrtkaId, String nazivTvrtke) {

    public static KorisnikResponse iz(Korisnik k) {
        return new KorisnikResponse(k.getId(), k.getIme(), k.getEmail(), k.getUloga(),
                k.getTvrtka().getId(), k.getTvrtka().getNaziv());
    }
}
