package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;

public record ClanTvrtkeResponse(Long id, String ime, String email, UlogaKorisnika uloga) {

    public static ClanTvrtkeResponse iz(Korisnik k) {
        return new ClanTvrtkeResponse(k.getId(), k.getIme(), k.getEmail(), k.getUloga());
    }
}
