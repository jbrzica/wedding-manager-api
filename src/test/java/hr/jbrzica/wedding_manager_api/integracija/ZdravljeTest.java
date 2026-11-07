package hr.jbrzica.wedding_manager_api.integracija;

import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ZdravljeTest {

    @Autowired
    MockMvc mvc;

    @Test
    void provjeraZdravljaRadiBezPrijave() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void ostaliActuatorEndpointiNisuDostupni() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError());
    }
}
