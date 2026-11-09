package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.IzvorUpita;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatistikaResponse(
        int godina,
        SazetakGodine ovaGodina,
        SazetakGodine proslaGodina,
        List<Mjesec> poMjesecima,
        List<Izvor> izvori,
        List<Stavka> paketi,
        List<Stavka> dodaci,
        List<Subota> subote,
        List<Honorar> honorari,
        Upiti upiti,
        BigDecimal zaNaplatiti,
        List<BigDecimal> proslaGodinaPoMjesecima
) {
    // Svi upisani termini godine: koliko je postalo posao, koliko otkazano, koliko još čeka odgovor
    public record Upiti(long ukupno, long posao, long otkazano, long otvoreno) {
    }

    public record SazetakGodine(long broj, BigDecimal prihod, BigDecimal zarada, BigDecimal prosjecnaCijena) {
    }

    public record Mjesec(int mjesec, long broj, BigDecimal dogovoreno, BigDecimal uplaceno) {
    }

    // izvor je null za vjenčanja kojima izvor nije upisan
    public record Izvor(IzvorUpita izvor, long upita, long posao) {
    }

    public record Stavka(String naziv, long broj) {
    }

    public record Subota(LocalDate datum, long dogovoreno, long upita) {
    }

    public record Honorar(String ime, long brojVjencanja, BigDecimal ukupno) {
    }
}
