package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.FinancijeResponse;
import hr.jbrzica.wedding_manager_api.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Kalkulator nema ovisnosti, pa mu ne trebaju mockovi.
class FinancijeKalkulatorTest {

    private final FinancijeKalkulator kalkulator = new FinancijeKalkulator();

    @Test
    void izracunaj_oduzimaUplateIHonorare() {
        // PRIPREMA: cijena 3250, kapara 1000, rata 500, videograf 800
        Vjencanje vjencanje = new Vjencanje();
        vjencanje.setDogovorenaCijena(new BigDecimal("3250.00"));
        vjencanje.getUplate().add(uplata("1000.00", VrstaUplate.KAPARA));
        vjencanje.getUplate().add(uplata("500.00", VrstaUplate.RATA));
        ClanEkipe videograf = new ClanEkipe();
        videograf.setHonorar(new BigDecimal("800.00"));
        vjencanje.getEkipa().add(videograf);

        // IZVRŠI
        FinancijeResponse f = kalkulator.izracunaj(vjencanje);

        // PROVJERI
        assertEquals(new BigDecimal("1500.00"), f.uplaceno());
        assertEquals(new BigDecimal("1750.00"), f.preostalo());
        assertTrue(f.kaparaPlacena());
        assertEquals(new BigDecimal("800.00"), f.honorariEkipe());
        assertEquals(new BigDecimal("2450.00"), f.zarada());
    }

    @Test
    void izracunaj_bezUplata_kaparaNijePlacena() {
        Vjencanje vjencanje = new Vjencanje();
        vjencanje.setDogovorenaCijena(new BigDecimal("2500.00"));

        FinancijeResponse f = kalkulator.izracunaj(vjencanje);

        assertEquals(new BigDecimal("2500.00"), f.preostalo());
        assertFalse(f.kaparaPlacena());
    }

    @Test
    void predlozenaCijena_zbrajaPaketIDodatke() {
        Paket paket = new Paket();
        paket.setCijena(new BigDecimal("2500.00"));

        BigDecimal cijena = kalkulator.predlozenaCijena(paket,
                List.of(dodatak("300.00"), dodatak("450.00")));

        assertEquals(new BigDecimal("3250.00"), cijena);
    }

    @Test
    void predlozenaCijena_bezPaketa_zbrajaSamoDodatke() {
        BigDecimal cijena = kalkulator.predlozenaCijena(null, List.of(dodatak("300.00")));

        assertEquals(new BigDecimal("300.00"), cijena);
    }

    private Uplata uplata(String iznos, VrstaUplate vrsta) {
        Uplata uplata = new Uplata();
        uplata.setIznos(new BigDecimal(iznos));
        uplata.setVrsta(vrsta);
        return uplata;
    }

    private Dodatak dodatak(String cijena) {
        Dodatak dodatak = new Dodatak();
        dodatak.setCijena(new BigDecimal(cijena));
        return dodatak;
    }
}
