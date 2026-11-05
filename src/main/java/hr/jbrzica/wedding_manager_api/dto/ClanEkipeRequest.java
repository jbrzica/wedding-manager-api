package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.UlogaSuradnika;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// Ako uloga nije poslana, uzima se zadana uloga suradnika.
public record ClanEkipeRequest(
        @NotNull(message = "Suradnik je obavezan") Long suradnikId,
        UlogaSuradnika uloga,
        @PositiveOrZero(message = "Honorar ne smije biti negativan") BigDecimal honorar
) {
}
