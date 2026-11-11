package hr.jbrzica.wedding_manager_api.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

// Početni zaslon: sljedeće vjenčanje i ono što traži pažnju
public record PregledResponse(
        VjencanjeSazetak sljedeceVjencanje,
        Long danaDoSljedeceg,
        LocalTime pocetakSljedeceg,
        int ekipaSljedeceg,
        long brojOveGodine,
        BigDecimal ukupnoNenaplaceno,
        long kasniIsporuka,
        long bezKapare,
        long noviUpiti
) {
}
