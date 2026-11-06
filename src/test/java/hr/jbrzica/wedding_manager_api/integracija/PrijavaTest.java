package hr.jbrzica.wedding_manager_api.integracija;

import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PrijavaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Test
    void ispravnaLozinkaVracaToken() throws Exception {
        Korisnik k = podaci.noviVlasnik("Studio", "tocna-lozinka");
        prijava(k.getEmail(), "tocna-lozinka")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void nakonPetKrivihLozinkiBlokiraCakIIspravnu() throws Exception {
        Korisnik k = podaci.noviVlasnik("Studio", "tocna-lozinka");
        for (int i = 0; i < 5; i++) {
            prijava(k.getEmail(), "kriva").andExpect(status().isUnauthorized());
        }
        prijava(k.getEmail(), "kriva").andExpect(status().isTooManyRequests());
        // Napadač ne smije moći provjeravati lozinke ni kad pogodi pravu
        prijava(k.getEmail(), "tocna-lozinka").andExpect(status().isTooManyRequests());
    }

    private ResultActions prijava(String email, String lozinka) throws Exception {
        return mvc.perform(post("/api/auth/prijava")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"lozinka\": \"" + lozinka + "\"}"));
    }
}
