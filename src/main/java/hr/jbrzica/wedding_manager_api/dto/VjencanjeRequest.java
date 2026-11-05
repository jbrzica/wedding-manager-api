package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.IzvorUpita;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Ako dogovorenaCijena nije poslana, računa se kao cijena paketa + dodataka.
public record VjencanjeRequest(
        @NotNull(message = "Datum je obavezan") LocalDate datum,
        String lokacija,
        @NotNull(message = "Podaci o mladoj su obavezni") @Valid OsobaDto mlada,
        @NotNull(message = "Podaci o mladoženji su obavezni") @Valid OsobaDto mladozenja,
        Long paketId,
        List<Long> dodatakIds,
        @PositiveOrZero(message = "Cijena ne smije biti negativna") BigDecimal dogovorenaCijena,
        LocalDate rokIsporuke,
        IzvorUpita izvorUpita,
        String napomene,
        List<@Valid StavkaRasporedaDto> raspored,
        List<@Valid ClanEkipeRequest> ekipa
) {
}
