package hr.jbrzica.wedding_manager_api.integracija;

import com.jayway.jsonpath.JsonPath;
import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PovijestTest {

    private static final String VJENCANJE = """
            {"datum": "2027-06-12", "lokacija": "Hotel Osijek",
             "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
             "dogovorenaCijena": %d, "dodatakIds": [], "raspored": [], "ekipa": []}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String token;
    private long vjencanje;

    // Upiše se vjenčanje, promijeni cijena, doda kapara (koja potvrdi termin) i premjesti u koš
    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        String odgovor = mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(VJENCANJE.formatted(2200)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        vjencanje = ((Number) JsonPath.read(odgovor, "$.id")).longValue();
        mvc.perform(put("/api/vjencanja/" + vjencanje).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(VJENCANJE.formatted(2400)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/vjencanja/" + vjencanje + "/uplate").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iznos\": 300, \"datum\": \"2026-11-09\", \"vrsta\": \"KAPARA\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void povijestVjencanjaImaSveBitnePromjeneOdNajnovije() throws Exception {
        mvc.perform(get("/api/vjencanja/" + vjencanje + "/povijest").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].opis").value("Status: Upit → Potvrđeno (plaćena kapara)"))
                .andExpect(jsonPath("$[1].opis").value("Dodana uplata: kapara 300 €"))
                .andExpect(jsonPath("$[2].opis").value("Cijena: 2.200 € → 2.400 €"))
                .andExpect(jsonPath("$[3].opis").value("Upisano vjenčanje"))
                .andExpect(jsonPath("$[0].korisnik").value("Vlasnik"));
    }

    @Test
    void povijestTvrtkePamtiIObrisanoVjencanje() throws Exception {
        mvc.perform(delete("/api/vjencanja/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/kos/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/povijest").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].opis").value("Obrisano zauvijek"))
                .andExpect(jsonPath("$[0].vjencanje").value("Ana & Ivo"))
                .andExpect(jsonPath("$[0].stanje").value("OBRISANO"))
                .andExpect(jsonPath("$[*].opis", hasItem("Premješteno u koš")));
    }

    @Test
    void vjencanjeUKosuImaStanjeUKosu() throws Exception {
        mvc.perform(get("/api/povijest").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].stanje").value("AKTIVNO"));
        mvc.perform(delete("/api/vjencanja/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/povijest").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].stanje").value("U_KOSU"));
    }

    @Test
    void drugaTvrtkaNeVidiTudjuPovijest() throws Exception {
        String tudji = jwtServis.napravi(podaci.noviVlasnik("Drugi", "lozinka123"));
        mvc.perform(get("/api/povijest").header("Authorization", "Bearer " + tudji))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/vjencanja/" + vjencanje + "/povijest").header("Authorization", "Bearer " + tudji))
                .andExpect(status().isNotFound());
    }
}
