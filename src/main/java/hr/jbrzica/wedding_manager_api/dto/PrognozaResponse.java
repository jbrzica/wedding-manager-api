package hr.jbrzica.wedding_manager_api.dto;

import java.time.LocalDate;

// kod je WMO šifra vremena (0 vedro, 61 kiša, 95 grmljavina...), oborine su vjerojatnost u postocima
public record PrognozaResponse(LocalDate datum, String mjesto, int kod, double max, double min, Integer oborine) {
}
