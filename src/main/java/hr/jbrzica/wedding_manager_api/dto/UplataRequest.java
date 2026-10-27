package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UplataRequest(
        @NotNull(message = "Iznos je obavezan")
        @Positive(message = "Iznos mora biti veći od 0") BigDecimal iznos,
        @NotNull(message = "Datum je obavezan") LocalDate datum,
        String opis
) {
}