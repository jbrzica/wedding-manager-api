package hr.jbrzica.wedding_manager_api.integracija;

import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prijava daje kratki access token i refresh token u HttpOnly kolačiću.
 * Refresh token se kod svakog osvježavanja zamjenjuje novim; stari se ne može ponovno upotrijebiti.
 */
// Bez razdoblja milosti: ovdje se provjerava da ponovna upotreba starog kolačića odmah opoziva prijavu
@SpringBootTest(properties = "app.refresh.milost-sekundi=0")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OsvjezavanjeTest {

    private static final String KOLACIC = "wm_osvjezi";

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    private Korisnik korisnik;

    @BeforeEach
    void pripremi() {
        korisnik = podaci.noviVlasnik("Studio", "tocna-lozinka");
    }

    @Test
    void prijavaPostavljaSigurniKolacic() throws Exception {
        mvc.perform(prijava())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString(KOLACIC + "="), containsString("HttpOnly"),
                        containsString("SameSite=Strict"), containsString("Path=/api/auth"))));
    }

    @Test
    void osvjezavanjeDajeNoviTokenINoviKolacic() throws Exception {
        String prvi = kolacic(mvc.perform(prijava()).andReturn().getResponse());
        MockHttpServletResponse odgovor = mvc.perform(osvjezi(prvi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.korisnik.email").value(korisnik.getEmail()))
                .andReturn().getResponse();
        assertNotEquals(prvi, kolacic(odgovor));
    }

    @Test
    void ponovnaUpotrebaStarogKolacicaOpozivaCijeluPrijavu() throws Exception {
        String prvi = kolacic(mvc.perform(prijava()).andReturn().getResponse());
        String drugi = kolacic(mvc.perform(osvjezi(prvi)).andReturn().getResponse());

        // Netko je ukrao i upotrijebio stari token: odbija se, a opoziva se i novi
        mvc.perform(osvjezi(prvi)).andExpect(status().isUnauthorized());
        mvc.perform(osvjezi(drugi)).andExpect(status().isUnauthorized());
    }

    @Test
    void odjavaOpozivaKolacic() throws Exception {
        String prvi = kolacic(mvc.perform(prijava()).andReturn().getResponse());
        mvc.perform(post("/api/auth/odjava").cookie(new Cookie(KOLACIC, prvi)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        mvc.perform(osvjezi(prvi)).andExpect(status().isUnauthorized());
    }

    @Test
    void bezKolacicaNemaOsvjezavanja() throws Exception {
        mvc.perform(post("/api/auth/osvjezi")).andExpect(status().isUnauthorized());
        mvc.perform(osvjezi("izmisljeni-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void promjenaLozinkeOdjavljujeDrugeUredaje() throws Exception {
        MockHttpServletResponse laptop = mvc.perform(prijava()).andReturn().getResponse();
        String mobitel = kolacic(mvc.perform(prijava()).andReturn().getResponse());
        String token = com.jayway.jsonpath.JsonPath.read(laptop.getContentAsString(), "$.token");

        mvc.perform(post("/api/auth/promjena-lozinke")
                        .header("Authorization", "Bearer " + token)
                        .cookie(new Cookie(KOLACIC, kolacic(laptop)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staraLozinka\": \"tocna-lozinka\", \"novaLozinka\": \"nova-lozinka-123\"}"))
                .andExpect(status().isOk());

        mvc.perform(osvjezi(mobitel)).andExpect(status().isUnauthorized());
        mvc.perform(osvjezi(kolacic(laptop))).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.RequestBuilder prijava() {
        return post("/api/auth/prijava").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"lozinka\": \"tocna-lozinka\"}".formatted(korisnik.getEmail()));
    }

    private org.springframework.test.web.servlet.RequestBuilder osvjezi(String vrijednost) {
        return post("/api/auth/osvjezi").cookie(new Cookie(KOLACIC, vrijednost));
    }

    private static String kolacic(MockHttpServletResponse odgovor) {
        Cookie c = odgovor.getCookie(KOLACIC);
        if (c == null) {
            throw new AssertionError("Odgovor nema kolačić " + KOLACIC);
        }
        return c.getValue();
    }
}
