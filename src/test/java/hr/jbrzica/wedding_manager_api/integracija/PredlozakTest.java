package hr.jbrzica.wedding_manager_api.integracija;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PredlozakTest {

    private static final String PREDLOZAK = """
            [{"vrijeme": "11:00", "naziv": "Priprema kod mladenke"},
             {"vrijeme": "15:00", "naziv": "Crkva", "adresa": "Katedrala, Đakovo"},
             {"vrijeme": "18:30", "naziv": "Sala"}]
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String token;

    @BeforeEach
    void pripremi() {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
    }

    @Test
    void spremljeniPredlozakSeVracaIstimRedom() throws Exception {
        mvc.perform(get("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PREDLOZAK))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].naziv").value("Crkva"))
                .andExpect(jsonPath("$[1].adresa").value("Katedrala, Đakovo"));
    }

    @Test
    void noviPredlozakZamjenjujeStariAPrazanGaBrise() throws Exception {
        mvc.perform(put("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(PREDLOZAK));
        mvc.perform(put("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("[{\"naziv\": \"Samo crkva\"}]"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(put("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void drugaTvrtkaNeVidiTudjiPredlozak() throws Exception {
        mvc.perform(put("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(PREDLOZAK));
        String tudji = jwtServis.napravi(podaci.noviVlasnik("Drugi", "lozinka123"));
        mvc.perform(get("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + tudji))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void stavkaBezNazivaSeOdbija() throws Exception {
        mvc.perform(put("/api/tvrtka/predlozak-rasporeda").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("[{\"vrijeme\": \"15:00\", \"naziv\": \"\"}]"))
                .andExpect(status().isBadRequest());
    }
}
