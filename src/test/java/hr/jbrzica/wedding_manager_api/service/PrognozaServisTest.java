package hr.jbrzica.wedding_manager_api.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrognozaServisTest {

    @Test
    void mjestoIzaZarezaIdePrvo() {
        assertEquals(List.of("Đakovo", "Rubin"), PrognozaServis.kandidati("Restoran Rubin, Đakovo"));
    }

    @Test
    void vrstaMjestaNijeGrad() {
        assertEquals(List.of("Osijek"), PrognozaServis.kandidati("Hotel Osijek"));
    }

    @Test
    void postanskiBrojSeMice() {
        assertEquals(List.of("Valpovo", "Prandau"), PrognozaServis.kandidati("Dvorac Prandau, 31550 Valpovo"));
    }

    @Test
    void samoImeMjesta() {
        assertEquals(List.of("Kumrovec"), PrognozaServis.kandidati("Kumrovec"));
    }

    @Test
    void prognozaSamoZaIducihSedamDana() {
        assertTrue(PrognozaServis.uRasponu(LocalDate.now().plusDays(3)));
        assertFalse(PrognozaServis.uRasponu(LocalDate.now().plusDays(20)));
        assertFalse(PrognozaServis.uRasponu(LocalDate.now().minusDays(1)));
    }

    @Test
    void iskljucenaPrognozaNistaNeVraca() {
        assertTrue(new PrognozaServis(false).za("Hotel Osijek", LocalDate.now().plusDays(1)).isEmpty());
    }
}
