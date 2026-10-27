package hr.jbrzica.wedding_manager_api.dto;

import java.math.BigDecimal;

public record DugResponse(
        BigDecimal ukupnaCijena,
        BigDecimal uplaceno,
        BigDecimal dug
) {
}