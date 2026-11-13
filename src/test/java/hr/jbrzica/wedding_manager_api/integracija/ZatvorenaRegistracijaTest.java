package hr.jbrzica.wedding_manager_api.integracija;

import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
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

// Na serveru je registracija zatvorena dok se ne uključi REGISTRACIJA_OTVORENA
@SpringBootTest(properties = "app.registracija.otvorena=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ZatvorenaRegistracijaTest {

    @Autowired
    MockMvc mvc;

    @Test
    void zatvorenaRegistracijaOdbijaNoveTvrtke() throws Exception {
        mvc.perform(get("/api/auth/postavke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registracijaOtvorena").value(false));
        mvc.perform(post("/api/auth/registracija").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nazivTvrtke": "Uljez", "ime": "X", "email": "uljez@test.hr", "lozinka": "lozinka123"}
                        """))
                .andExpect(status().isForbidden());
    }
}
