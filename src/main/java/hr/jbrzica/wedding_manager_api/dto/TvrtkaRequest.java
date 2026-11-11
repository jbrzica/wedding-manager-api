package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TvrtkaRequest(
        @NotBlank(message = "Naziv je obavezan") String naziv,
        @Size(max = 255, message = "Adresa je preduga") String adresa,
        @Pattern(regexp = "(\\d{11})?", message = "OIB mora imati točno 11 znamenki") String oib,
        @Size(max = 50, message = "Telefon je predug") String telefon,
        @Email(message = "Email nije ispravan") String email
) {
}
