package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;

import java.math.BigDecimal;

public record DodatakResponse(Long id, String naziv, BigDecimal cijena) {

    public static DodatakResponse iz(Dodatak dodatak) {
        return new DodatakResponse(dodatak.getId(), dodatak.getNaziv(), dodatak.getCijena());
    }
}
