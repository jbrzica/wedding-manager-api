package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Upit zapisan usred razgovora: samo ono što se zna odmah. Ostalo se dopuni kad par potvrdi.
public record BrziUpitRequest(
        @NotNull(message = "Datum je obavezan") LocalDate datum,
        @NotBlank(message = "Ime je obavezno") @Size(max = 200, message = "Ime je predugo") String ime,
        @Size(max = 50, message = "Telefon je predug") String telefon,
        @Size(max = 2000, message = "Napomena je preduga") String napomena
) {
}
