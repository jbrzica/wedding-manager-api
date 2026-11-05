package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UplataResponse(Long id, BigDecimal iznos, LocalDate datum, VrstaUplate vrsta, String opis) {

    public static UplataResponse iz(Uplata u) {
        return new UplataResponse(u.getId(), u.getIznos(), u.getDatum(), u.getVrsta(), u.getOpis());
    }
}
