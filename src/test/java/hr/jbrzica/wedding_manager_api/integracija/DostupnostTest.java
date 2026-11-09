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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DostupnostTest {

    private static final LocalDate SUBOTA = LocalDate.of(2027, 6, 12);

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String token;

    // Subota: vjenčanje sa snimateljem; nedjelja: drugo vjenčanje; petak: otkazano
    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        long snimatelj = suradnik("Marko");
        suradnik("Pero");
        novo(SUBOTA, "Ana", snimatelj);
        novo(SUBOTA.plusDays(1), "Iva", null);
        long otkazano = novo(SUBOTA.minusDays(1), "Maja", null);
        mvc.perform(patch("/api/vjencanja/" + otkazano + "/status?status=OTKAZANO").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void zauzetDanPokazujeVjencanjeIZauzeteSuradnike() throws Exception {
        mvc.perform(get("/api/vjencanja/dostupnost?datum=" + SUBOTA).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.slobodno").value(false))
                .andExpect(jsonPath("$.vjencanja", hasSize(1)))
                .andExpect(jsonPath("$.blizu", hasSize(1)))
                .andExpect(jsonPath("$.suradnici", hasSize(2)))
                .andExpect(jsonPath("$.suradnici[0].ime").value("Marko"))
                .andExpect(jsonPath("$.suradnici[0].naVjencanju").value(org.hamcrest.Matchers.startsWith("Ana")))
                .andExpect(jsonPath("$.suradnici[1].naVjencanju").value(nullValue()));
    }

    @Test
    void otkazanoVjencanjeNeZauzimaDan() throws Exception {
        mvc.perform(get("/api/vjencanja/dostupnost?datum=" + SUBOTA.minusDays(1)).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.slobodno").value(true))
                .andExpect(jsonPath("$.vjencanja", hasSize(0)));
    }

    @Test
    void drugaTvrtkaVidiSlobodanDan() throws Exception {
        String tudji = jwtServis.napravi(podaci.noviVlasnik("Drugi", "lozinka123"));
        mvc.perform(get("/api/vjencanja/dostupnost?datum=" + SUBOTA).header("Authorization", "Bearer " + tudji))
                .andExpect(jsonPath("$.slobodno").value(true))
                .andExpect(jsonPath("$.suradnici", hasSize(0)));
    }

    private long suradnik(String ime) throws Exception {
        String odgovor = mvc.perform(post("/api/suradnici").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ime\": \"%s\", \"uloga\": \"VIDEOGRAF\"}".formatted(ime)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }

    private long novo(LocalDate datum, String mlada, Long suradnik) throws Exception {
        String ekipa = suradnik == null ? "" : "{\"suradnikId\": %d}".formatted(suradnik);
        String odgovor = mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datum": "%s", "mlada": {"ime": "%s", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                                 "dogovorenaCijena": 1000, "dodatakIds": [], "raspored": [], "ekipa": [%s]}
                                """.formatted(datum, mlada, ekipa)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }
}
