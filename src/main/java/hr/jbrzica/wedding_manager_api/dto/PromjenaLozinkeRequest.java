package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromjenaLozinkeRequest(
        @NotBlank(message = "Trenutna lozinka je obavezna") String staraLozinka,
        @NotBlank(message = "Nova lozinka je obavezna")
        @Size(min = 8, message = "Lozinka mora imati barem 8 znakova") String novaLozinka
) {
}
