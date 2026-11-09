package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Promjena;

import java.time.Instant;

// stanje vjenčanja danas: AKTIVNO (link na vjenčanje), U_KOSU (link u koš) ili OBRISANO (bez linka)
public record PromjenaResponse(Long id, Instant vrijeme, String korisnik, Long vjencanjeId, String vjencanje,
                               String opis, String stanje) {

    public static PromjenaResponse iz(Promjena p, String stanje) {
        return new PromjenaResponse(p.getId(), p.getVrijeme(), p.getKorisnikIme(), p.getVjencanjeId(),
                p.getVjencanjeNaziv(), p.getOpis(), stanje);
    }
}
