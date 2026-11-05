package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrihvatiPozivRequest(
        @NotBlank(message = "Email je obavezan") @Email(message = "Email nije ispravan") String email,
        @NotBlank(message = "Šifra je obavezna") String kod,
        @NotBlank(message = "Ime je obavezno") String ime,
        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 8, message = "Lozinka mora imati barem 8 znakova") String lozinka
) {
}
