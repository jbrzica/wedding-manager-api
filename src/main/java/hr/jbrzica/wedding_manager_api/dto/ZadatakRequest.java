package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotBlank;

public record ZadatakRequest(@NotBlank(message = "Naziv je obavezan") String naziv) {
}
