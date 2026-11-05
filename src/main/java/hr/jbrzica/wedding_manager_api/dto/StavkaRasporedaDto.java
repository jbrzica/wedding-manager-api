package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.StavkaRasporeda;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

public record StavkaRasporedaDto(
        LocalTime vrijeme,
        @NotBlank(message = "Naziv stavke je obavezan") String naziv,
        String adresa,
        String napomena
) {

    public static StavkaRasporedaDto iz(StavkaRasporeda s) {
        return new StavkaRasporedaDto(s.getVrijeme(), s.getNaziv(), s.getAdresa(), s.getNapomena());
    }
}
