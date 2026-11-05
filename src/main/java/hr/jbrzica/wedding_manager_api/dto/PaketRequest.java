package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record PaketRequest(
        @NotBlank(message = "Naziv je obavezan") String naziv,
        String opis,
        @NotNull(message = "Cijena je obavezna")
        @PositiveOrZero(message = "Cijena ne smije biti negativna") BigDecimal cijena
) {
}
