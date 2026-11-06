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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Najvažnije sigurnosno pravilo aplikacije: tvrtka B ne smije vidjeti ni mijenjati podatke tvrtke A.
 * Testira se kroz pravi HTTP put (filter, controller, servis, baza), ne samo jedna klasa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IzolacijaTvrtkiTest {

    private static final String VJENCANJE = """
            {
              "datum": "2027-06-12",
              "lokacija": "Hotel Osijek",
              "mlada": {"ime": "Ana", "prezime": "Anić"},
              "mladozenja": {"ime": "Marko", "prezime": "Marić"},
              "dogovorenaCijena": 1500,
              "dodatakIds": [],
              "raspored": [],
              "ekipa": []
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String tokenA;
    private String tokenB;
    private long vjencanjeA;

    @BeforeEach
    void pripremi() throws Exception {
        tokenA = jwtServis.napravi(podaci.noviVlasnik("Studio A", "lozinka123"));
        tokenB = jwtServis.napravi(podaci.noviVlasnik("Studio B", "lozinka123"));

        String odgovor = mvc.perform(post("/api/vjencanja")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VJENCANJE))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        vjencanjeA = ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }

    @Test
    void vlasnikVidiSvojeVjencanje() throws Exception {
        mvc.perform(get("/api/vjencanja/" + vjencanjeA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lokacija").value("Hotel Osijek"));
    }

    @Test
    void tudjeVjencanjeVraca404() throws Exception {
        mvc.perform(get("/api/vjencanja/" + vjencanjeA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void tudjeVjencanjeNijeUPopisu() throws Exception {
        mvc.perform(get("/api/vjencanja?razdoblje=SVA").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/vjencanja?razdoblje=SVA").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void tudjeVjencanjeSeNeMozeUreditNiObrisati() throws Exception {
        mvc.perform(put("/api/vjencanja/" + vjencanjeA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VJENCANJE.replace("Hotel Osijek", "Hakirano")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/vjencanja/" + vjencanjeA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Vjenčanje je i dalje tu i nepromijenjeno
        mvc.perform(get("/api/vjencanja/" + vjencanjeA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lokacija").value("Hotel Osijek"));
    }

    @Test
    void uplataNaTudjeVjencanjeVraca404() throws Exception {
        mvc.perform(post("/api/vjencanja/" + vjencanjeA + "/uplate")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iznos": 300, "datum": "2026-11-06", "vrsta": "KAPARA"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void tudjiPaketVraca404() throws Exception {
        String odgovor = mvc.perform(post("/api/paketi")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"naziv": "Cijeli dan", "cijena": 1500}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long paketA = ((Number) JsonPath.read(odgovor, "$.id")).longValue();

        mvc.perform(get("/api/paketi/" + paketA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void bezTokenaVraca401() throws Exception {
        mvc.perform(get("/api/vjencanja"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void neispravanTokenVraca401() throws Exception {
        mvc.perform(get("/api/vjencanja").header("Authorization", "Bearer nije.pravi.token"))
                .andExpect(status().isUnauthorized());
    }
}
