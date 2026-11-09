package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Promjena;

import java.time.Instant;

public record PromjenaResponse(Long id, Instant vrijeme, String korisnik, Long vjencanjeId, String vjencanje, String opis) {

    public static PromjenaResponse iz(Promjena p) {
        return new PromjenaResponse(p.getId(), p.getVrijeme(), p.getKorisnikIme(), p.getVjencanjeId(),
                p.getVjencanjeNaziv(), p.getOpis());
    }
}
