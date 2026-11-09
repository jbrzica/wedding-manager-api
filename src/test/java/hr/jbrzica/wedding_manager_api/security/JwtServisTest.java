package hr.jbrzica.wedding_manager_api.security;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServisTest {

    private static final String TAJNA = "YYARpuOZfXWsInxPJx8PvqMwtynfByH6Y17xyJwnUDOk4+G1nsXjaxwNxFTHcA0+";
    private static final String DRUGA_TAJNA = "IMjAoz2jEC8TVZsBz5xbQAj+jGAsWix0gRduBd0kwULLwRbZilYYDFFeGqV925iu";

    private final JwtServis jwtServis = new JwtServis(TAJNA, 15);

    @Test
    void napravljeniToken_vracaIdKorisnika() {
        String token = jwtServis.napravi(korisnik(42L));

        assertEquals(Long.valueOf(42), jwtServis.procitajKorisnikId(token));
    }

    @Test
    void tokenPotpisanDrugimKljucem_seOdbija() {
        String tudiToken = new JwtServis(DRUGA_TAJNA, 15).napravi(korisnik(42L));

        assertThrows(JwtException.class, () -> jwtServis.procitajKorisnikId(tudiToken));
    }

    @Test
    void neispravanToken_seOdbija() {
        assertThrows(JwtException.class, () -> jwtServis.procitajKorisnikId("ovo.nije.token"));
    }

    private Korisnik korisnik(Long id) {
        Korisnik korisnik = new Korisnik();
        korisnik.setId(id);
        return korisnik;
    }
}
