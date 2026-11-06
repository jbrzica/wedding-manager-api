package hr.jbrzica.wedding_manager_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrijavaLimiterTest {

    private static final String EMAIL = "tata@test.hr";

    // Sat koji test sam pomiče, umjesto da stvarno čeka 15 minuta.
    static class TestSat extends Clock {
        Instant sada = Instant.parse("2026-11-06T10:00:00Z");

        void pomakni(Duration d) {
            sada = sada.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return sada;
        }
    }

    private final TestSat sat = new TestSat();
    private final PrijavaLimiter limiter = new PrijavaLimiter(sat);

    @Test
    void cetiriKriveLozinkeJosPusta() {
        neuspjeha(4);
        assertDoesNotThrow(() -> limiter.provjeri(EMAIL));
    }

    @Test
    void petKrivihLozinkiBlokiraS429() {
        neuspjeha(5);
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> limiter.provjeri(EMAIL));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
    }

    @Test
    void nakon15MinutaOpetPusta() {
        neuspjeha(5);
        sat.pomakni(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> limiter.provjeri(EMAIL));
    }

    @Test
    void uspjesnaPrijavaBriseBrojanje() {
        neuspjeha(4);
        limiter.uspjeh(EMAIL);
        neuspjeha(4);
        assertDoesNotThrow(() -> limiter.provjeri(EMAIL));
    }

    @Test
    void blokadaVrijediSamoZaTajEmail() {
        neuspjeha(5);
        assertDoesNotThrow(() -> limiter.provjeri("netko.drugi@test.hr"));
    }

    private void neuspjeha(int broj) {
        for (int i = 0; i < broj; i++) {
            limiter.neuspjeh(EMAIL);
            sat.pomakni(Duration.ofSeconds(10));
        }
    }
}
