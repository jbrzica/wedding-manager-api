package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Vjencanje;

import java.time.Instant;
import java.time.LocalDate;

public record KosStavka(Long id, LocalDate datum, String mlada, String mladozenja, String lokacija,
                        Instant obrisano, Instant trajnoBrisanje) {

    public static KosStavka iz(Vjencanje v, Instant trajnoBrisanje) {
        return new KosStavka(v.getId(), v.getDatum(),
                v.getMlada() != null ? v.getMlada().punoIme() : null,
                v.getMladozenja() != null ? v.getMladozenja().punoIme() : null,
                v.getLokacija(), v.getObrisano(), trajnoBrisanje);
    }
}
