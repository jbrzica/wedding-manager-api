package hr.jbrzica.wedding_manager_api.integracija;

import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PrviKoraciTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    @Test
    void koraciSeOznacavajuPremaPodacima() throws Exception {
        String token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        mvc.perform(get("/api/tvrtka/prvi-koraci").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.paketi").value(false))
                .andExpect(jsonPath("$.suradnici").value(false))
                .andExpect(jsonPath("$.vjencanje").value(false))
                .andExpect(jsonPath("$.kalendar").value(false));

        mvc.perform(post("/api/paketi").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"naziv\": \"Cijeli dan\", \"cijena\": 1500}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/kalendar/link").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/tvrtka/prvi-koraci").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.paketi").value(true))
                .andExpect(jsonPath("$.kalendar").value(true))
                .andExpect(jsonPath("$.vjencanje").value(false))
                .andExpect(jsonPath("$.brojVjencanja").value(0));
    }
}
