package hr.jbrzica.wedding_manager_api.dto;

import java.math.BigDecimal;

public record PregledResponse(
        VjencanjeSazetak sljedeceVjencanje,
        Long danaDoSljedeceg,
        long brojOveGodine,
        BigDecimal ukupnoNenaplaceno
) {
}
