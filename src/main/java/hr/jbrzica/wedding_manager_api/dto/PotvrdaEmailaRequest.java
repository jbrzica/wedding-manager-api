package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PotvrdaEmailaRequest(
        @NotBlank(message = "Email je obavezan") @Email(message = "Email nije ispravan") String email,
        @NotBlank(message = "Šifra je obavezna") String kod
) {
}
