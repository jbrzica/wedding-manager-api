package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.UlogaSuradnika;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SuradnikRequest(
        @NotBlank(message = "Ime je obavezno") String ime,
        @NotNull(message = "Uloga je obavezna") UlogaSuradnika uloga,
        String telefon,
        @Email(message = "Email nije ispravan") String email,
        String napomena
) {
}
