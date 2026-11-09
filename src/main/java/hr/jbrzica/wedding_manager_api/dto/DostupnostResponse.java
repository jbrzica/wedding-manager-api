package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.UlogaSuradnika;

import java.time.LocalDate;
import java.util.List;

/**
 * Odgovor na pitanje „jeste li slobodni tog datuma?“.
 * slobodno = taj dan nema vjenčanja (otkazana se ne broje); upit se broji, jer termin možda bude potvrđen.
 */
public record DostupnostResponse(
        LocalDate datum,
        boolean slobodno,
        List<VjencanjeSazetak> vjencanja,
        List<VjencanjeSazetak> blizu,
        List<SuradnikDostupnost> suradnici
) {
    // naVjencanju je par na čijem je vjenčanju suradnik taj dan, ili null ako je slobodan
    public record SuradnikDostupnost(Long id, String ime, UlogaSuradnika uloga, String naVjencanju) {
    }
}
