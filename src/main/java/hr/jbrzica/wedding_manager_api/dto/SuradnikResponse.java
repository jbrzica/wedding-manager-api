package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Suradnik;
import hr.jbrzica.wedding_manager_api.entity.UlogaSuradnika;

public record SuradnikResponse(Long id, String ime, UlogaSuradnika uloga, String telefon,
                               String email, String napomena) {

    public static SuradnikResponse iz(Suradnik s) {
        return new SuradnikResponse(s.getId(), s.getIme(), s.getUloga(), s.getTelefon(),
                s.getEmail(), s.getNapomena());
    }
}
