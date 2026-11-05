package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.IzvorUpita;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;

import java.time.LocalDate;
import java.util.List;

public record VjencanjeDetalji(
        Long id,
        LocalDate datum,
        String lokacija,
        OsobaDto mlada,
        OsobaDto mladozenja,
        PaketResponse paket,
        List<DodatakResponse> dodaci,
        StatusVjencanja status,
        LocalDate rokIsporuke,
        IzvorUpita izvorUpita,
        String napomene,
        List<StavkaRasporedaDto> raspored,
        List<ClanEkipeResponse> ekipa,
        List<UplataResponse> uplate,
        FinancijeResponse financije
) {
}
