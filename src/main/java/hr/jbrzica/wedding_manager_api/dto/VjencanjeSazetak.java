package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;

import java.math.BigDecimal;
import java.time.LocalDate;

// Jedan red u popisu i kalendaru: samo ono što treba za prikaz.
public record VjencanjeSazetak(
        Long id,
        LocalDate datum,
        String lokacija,
        String mlada,
        String mladozenja,
        StatusVjencanja status,
        BigDecimal dogovorenaCijena,
        BigDecimal uplaceno,
        BigDecimal preostalo,
        boolean kaparaPlacena,
        String paket
) {
}
