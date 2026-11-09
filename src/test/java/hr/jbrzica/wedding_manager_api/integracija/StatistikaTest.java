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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatistikaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String token;

    // 2027.: dva potvrđena (1000 i 2000 €, uplaćeno 300 i 2000), jedan upit, jedno otkazano; 2026.: jedno potvrđeno
    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        uplata(novo("2027-06-12", 1000), 300);
        uplata(novo("2027-07-10", 2000), 2000);
        novo("2027-08-14", 1500);
        long otkazano = novo("2027-09-04", 1500);
        mvc.perform(patch("/api/vjencanja/" + otkazano + "/status?status=OTKAZANO").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        uplata(novo("2026-06-13", 1200), 300);
    }

    @Test
    void upitiZaNaplatitiIProslaGodina() throws Exception {
        mvc.perform(get("/api/statistika?godina=2027").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ovaGodina.broj").value(2))
                .andExpect(jsonPath("$.upiti.ukupno").value(4))
                .andExpect(jsonPath("$.upiti.posao").value(2))
                .andExpect(jsonPath("$.upiti.otkazano").value(1))
                .andExpect(jsonPath("$.upiti.otvoreno").value(1))
                .andExpect(jsonPath("$.zaNaplatiti").value(700))
                .andExpect(jsonPath("$.proslaGodinaPoMjesecima", hasSize(12)))
                .andExpect(jsonPath("$.proslaGodinaPoMjesecima[5]").value(1200));
    }

    private long novo(String datum, int cijena) throws Exception {
        String odgovor = mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datum": "%s", "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                                 "dogovorenaCijena": %d, "dodatakIds": [], "raspored": [], "ekipa": []}
                                """.formatted(datum, cijena)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }

    // Kapara ujedno potvrdi vjenčanje (iz upita prelazi u potvrđeno)
    private void uplata(long vjencanje, int iznos) throws Exception {
        mvc.perform(post("/api/vjencanja/" + vjencanje + "/uplate").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iznos\": %d, \"datum\": \"2026-11-09\", \"vrsta\": \"KAPARA\"}".formatted(iznos)))
                .andExpect(status().isCreated());
    }
}
