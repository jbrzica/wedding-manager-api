package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VjencanjeRequest(
        @NotNull(message = "Datum je obavezan") LocalDate datum,
        @NotBlank(message = "Lokacija je obavezna") String lokacija,
        @NotNull(message = "klijentId je obavezan") Long klijentId,
        @NotNull(message = "paketId je obavezan") Long paketId
) {
}