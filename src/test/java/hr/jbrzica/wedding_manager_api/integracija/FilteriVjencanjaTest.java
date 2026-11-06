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

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FilteriVjencanjaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String token;

    // Upit (neplaćen), Potvrđeno (plaćeno do kraja), Odrađeno kojem je rok isporuke prošao
    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));

        novo("Upit", "2027-05-01", null);

        long potvrdeno = novo("Placeno", "2027-06-01", null);
        mvc.perform(post("/api/vjencanja/" + potvrdeno + "/uplate").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iznos\": 1000, \"datum\": \"2026-11-06\", \"vrsta\": \"KAPARA\"}"))
                .andExpect(status().isCreated());

        long odradeno = novo("Kasni", LocalDate.now().minusMonths(3).toString(),
                LocalDate.now().minusDays(1).toString());
        mvc.perform(patch("/api/vjencanja/" + odradeno + "/status?status=ODRADENO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void viseStatusaOdjednom() throws Exception {
        mvc.perform(get("/api/vjencanja?razdoblje=SVA&status=UPIT&status=POTVRDENO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].lokacija", containsInAnyOrder("Upit", "Placeno")));
    }

    @Test
    void bezStatusaVracaSve() throws Exception {
        mvc.perform(get("/api/vjencanja?razdoblje=SVA").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void samoNenaplacena() throws Exception {
        mvc.perform(get("/api/vjencanja?razdoblje=SVA&nenaplaceno=true").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[*].lokacija", containsInAnyOrder("Upit", "Kasni")));
    }

    @Test
    void samoKasniIsporuka() throws Exception {
        mvc.perform(get("/api/vjencanja?razdoblje=SVA&kasniIsporuka=true").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lokacija").value("Kasni"));
    }

    private long novo(String lokacija, String datum, String rokIsporuke) throws Exception {
        String rok = rokIsporuke == null ? "null" : "\"" + rokIsporuke + "\"";
        String odgovor = mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datum": "%s", "lokacija": "%s", "rokIsporuke": %s,
                                 "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                                 "dogovorenaCijena": 1000, "dodatakIds": [], "raspored": [], "ekipa": []}
                                """.formatted(datum, lokacija, rok)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }
}
