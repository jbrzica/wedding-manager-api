package hr.jbrzica.wedding_manager_api.dto;

import java.math.BigDecimal;

public record FinancijeResponse(
        BigDecimal dogovorenaCijena,
        BigDecimal uplaceno,
        BigDecimal preostalo,
        boolean kaparaPlacena,
        BigDecimal honorariEkipe,
        BigDecimal zarada
) {
}
