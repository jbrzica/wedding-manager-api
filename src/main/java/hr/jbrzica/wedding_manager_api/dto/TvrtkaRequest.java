package hr.jbrzica.wedding_manager_api.dto;

import jakarta.validation.constraints.NotBlank;

public record TvrtkaRequest(@NotBlank(message = "Naziv je obavezan") String naziv) {
}
