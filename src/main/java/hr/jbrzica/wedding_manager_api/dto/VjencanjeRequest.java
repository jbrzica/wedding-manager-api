package hr.jbrzica.wedding_manager_api.dto;

import java.time.LocalDate;

public record VjencanjeRequest(
        LocalDate datum,
        String lokacija,
        Long klijentId,
        Long paketId
) {
}