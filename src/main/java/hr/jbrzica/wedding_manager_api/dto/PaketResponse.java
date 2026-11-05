package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Paket;

import java.math.BigDecimal;

public record PaketResponse(Long id, String naziv, String opis, BigDecimal cijena) {

    public static PaketResponse iz(Paket paket) {
        return new PaketResponse(paket.getId(), paket.getNaziv(), paket.getOpis(), paket.getCijena());
    }
}
