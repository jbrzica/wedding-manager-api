package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import hr.jbrzica.wedding_manager_api.entity.Osoba;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.UlogaSuradnika;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;

import java.time.LocalDate;
import java.util.List;

/**
 * Javna stranica suradnika. Namjerno nema nijednog polja s novcem (cijena, uplate, honorari),
 * pa ih ne može slučajno otkriti ni greška u frontendu.
 */
public record SuradnikPregledResponse(
        String ime,
        String tvrtka,
        String kalendar,
        List<VjencanjeSuradnika> vjencanja
) {

    public record VjencanjeSuradnika(
            LocalDate datum,
            String lokacija,
            boolean potvrdeno,
            UlogaSuradnika mojaUloga,
            Kontakt mlada,
            Kontakt mladozenja,
            List<StavkaRasporedaDto> raspored,
            List<Clan> ekipa
    ) {

        public static VjencanjeSuradnika iz(ClanEkipe moje) {
            Vjencanje v = moje.getVjencanje();
            Long mojId = moje.getSuradnik().getId();
            return new VjencanjeSuradnika(
                    v.getDatum(),
                    v.getLokacija(),
                    v.getStatus() != StatusVjencanja.UPIT,
                    moje.getUloga(),
                    Kontakt.iz(v.getMlada()),
                    Kontakt.iz(v.getMladozenja()),
                    v.getRaspored().stream().map(StavkaRasporedaDto::iz).toList(),
                    v.getEkipa().stream()
                            .map(c -> new Clan(c.getSuradnik().getIme(), c.getUloga(), c.getSuradnik().getTelefon(),
                                    c.getSuradnik().getId().equals(mojId)))
                            .toList());
        }
    }

    public record Kontakt(String ime, String telefon) {

        static Kontakt iz(Osoba o) {
            return o == null ? null : new Kontakt(o.punoIme(), o.getTelefon());
        }
    }

    public record Clan(String ime, UlogaSuradnika uloga, String telefon, boolean ja) {
    }
}
