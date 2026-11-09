package hr.jbrzica.wedding_manager_api.integracija;

import com.jayway.jsonpath.JsonPath;
import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import hr.jbrzica.wedding_manager_api.service.KosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Obrisano vjenčanje ide u koš: nestane iz aplikacije, može se vratiti, a nakon 30 dana briše se zauvijek.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class KosTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    @Autowired
    KosService kosService;

    @Autowired
    JdbcTemplate jdbc;

    private String token;
    private long vjencanje;

    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        String odgovor = mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datum": "%s", "lokacija": "Hotel",
                                 "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                                 "dogovorenaCijena": 1000, "dodatakIds": [],
                                 "raspored": [{"vrijeme": "15:00", "naziv": "Crkva"}], "ekipa": []}
                                """.formatted(LocalDate.now().plusMonths(2))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        vjencanje = ((Number) JsonPath.read(odgovor, "$.id")).longValue();
        mvc.perform(post("/api/vjencanja/" + vjencanje + "/uplate").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iznos\": 300, \"datum\": \"2026-11-09\", \"vrsta\": \"KAPARA\"}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/vjencanja/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void obrisanoNestaneIzAplikacijeIPojaviSeUKosu() throws Exception {
        mvc.perform(get("/api/vjencanja/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/vjencanja?razdoblje=SVA").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/kos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].mlada").value("Ana A"))
                .andExpect(jsonPath("$[0].trajnoBrisanje").isNotEmpty());
    }

    @Test
    void vracenoJeOpetTuSaSvimPodacima() throws Exception {
        mvc.perform(post("/api/kos/" + vjencanje + "/vrati").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/vjencanja/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raspored", hasSize(1)));
        mvc.perform(get("/api/vjencanja/" + vjencanje + "/uplate").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/kos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void trajnoBrisanjeBriseIUplate() throws Exception {
        mvc.perform(delete("/api/kos/" + vjencanje).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/kos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
        Integer uplata = jdbc.queryForObject("SELECT count(*) FROM uplata WHERE vjencanje_id = ?", Integer.class, vjencanje);
        org.junit.jupiter.api.Assertions.assertEquals(0, uplata);
    }

    @Test
    void drugaTvrtkaNeMozeVratitiNiObrisatiTudjeIzKosa() throws Exception {
        String tudji = jwtServis.napravi(podaci.noviVlasnik("Drugi", "lozinka123"));
        mvc.perform(post("/api/kos/" + vjencanje + "/vrati").header("Authorization", "Bearer " + tudji))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/kos/" + vjencanje).header("Authorization", "Bearer " + tudji))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/kos").header("Authorization", "Bearer " + tudji))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nakon30DanaSeBriseSamo() throws Exception {
        jdbc.update("UPDATE vjencanje SET obrisano = now() - interval '31 days' WHERE id = ?", vjencanje);
        kosService.isprazniStaro();
        Integer ostalo = jdbc.queryForObject("SELECT count(*) FROM vjencanje WHERE id = ?", Integer.class, vjencanje);
        org.junit.jupiter.api.Assertions.assertEquals(0, ostalo);
    }

    @Test
    void mladjeOd30DanaOstajeUKosu() throws Exception {
        jdbc.update("UPDATE vjencanje SET obrisano = now() - interval '29 days' WHERE id = ?", vjencanje);
        kosService.isprazniStaro();
        Integer ostalo = jdbc.queryForObject("SELECT count(*) FROM vjencanje WHERE id = ?", Integer.class, vjencanje);
        org.junit.jupiter.api.Assertions.assertEquals(1, ostalo);
    }
}
