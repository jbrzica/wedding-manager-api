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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suradnik preko tajnog linka vidi samo svoja nadolazeća vjenčanja i nikad ne vidi novac.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LinkSuradnikaTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    private String token;
    private long snimatelj;
    private String linkSnimatelja;

    // Snimatelj radi na dva buduća vjenčanja, jednom prošlom i jednom otkazanom; dron na petom.
    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        snimatelj = suradnik("Marko", "VIDEOGRAF");
        long dron = suradnik("Pero", "DRON");

        novo("Hotel Osijek", LocalDate.now().plusMonths(2), snimatelj, dron);
        novo("Dvorac Bizovac", LocalDate.now().plusMonths(5), snimatelj);
        novo("Prošlo", LocalDate.now().minusMonths(1), snimatelj);
        long otkazano = novo("Otkazano", LocalDate.now().plusMonths(3), snimatelj);
        mvc.perform(patch("/api/vjencanja/" + otkazano + "/status?status=OTKAZANO").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        novo("Samo dron", LocalDate.now().plusMonths(4), dron);

        String odgovor = mvc.perform(get("/api/suradnici/" + snimatelj + "/link").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        linkSnimatelja = JsonPath.read(odgovor, "$.putanja");
    }

    @Test
    void suradnikBezPrijaveVidiSamoSvojaNadolazecaVjencanja() throws Exception {
        mvc.perform(get(apiPutanja(linkSnimatelja)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ime").value("Marko"))
                .andExpect(jsonPath("$.vjencanja", hasSize(2)))
                .andExpect(jsonPath("$.vjencanja[*].lokacija", containsInAnyOrder("Hotel Osijek", "Dvorac Bizovac")))
                .andExpect(jsonPath("$.vjencanja[0].mojaUloga").value("VIDEOGRAF"))
                .andExpect(jsonPath("$.vjencanja[0].ekipa", hasSize(2)));
    }

    @Test
    void stranicaSuradnikaNemaNikakvihIznosa() throws Exception {
        mvc.perform(get(apiPutanja(linkSnimatelja)))
                .andExpect(content().string(not(containsString("cijena"))))
                .andExpect(content().string(not(containsString("honorar"))))
                .andExpect(content().string(not(containsString("uplac"))))
                .andExpect(content().string(not(containsString("1500"))));
    }

    @Test
    void kalendarSuradnikaImaSvaNjegovaVjencanjaOsimOtkazanog() throws Exception {
        String ics = mvc.perform(get(apiPutanja(linkSnimatelja) + "/kalendar.ics"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long dogadaja = ics.lines().filter("BEGIN:VEVENT"::equals).count();
        org.junit.jupiter.api.Assertions.assertEquals(3, dogadaja);
    }

    @Test
    void noviLinkPonistavaStari() throws Exception {
        mvc.perform(post("/api/suradnici/" + snimatelj + "/link/novi").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get(apiPutanja(linkSnimatelja)))
                .andExpect(status().isNotFound());
    }

    @Test
    void izmisljeniLinkVraca404() throws Exception {
        mvc.perform(get("/api/javno/suradnik/izmisljeni-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void drugaTvrtkaNeMozeDohvatitiLinkTudjegSuradnika() throws Exception {
        String tudji = jwtServis.napravi(podaci.noviVlasnik("Drugi studio", "lozinka123"));
        mvc.perform(get("/api/suradnici/" + snimatelj + "/link").header("Authorization", "Bearer " + tudji))
                .andExpect(status().isNotFound());
    }

    // Frontend link je /s/{token}, a podaci su na /api/javno/suradnik/{token}
    private String apiPutanja(String link) {
        return "/api/javno/suradnik/" + link.substring("/s/".length());
    }

    private long suradnik(String ime, String uloga) throws Exception {
        String odgovor = mvc.perform(post("/api/suradnici").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ime\": \"%s\", \"uloga\": \"%s\", \"telefon\": \"091 111 222\"}".formatted(ime, uloga)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }

    private long novo(String lokacija, LocalDate datum, long... suradnici) throws Exception {
        StringBuilder ekipa = new StringBuilder();
        for (long id : suradnici) {
            ekipa.append(ekipa.isEmpty() ? "" : ",").append("{\"suradnikId\": %d, \"honorar\": 250}".formatted(id));
        }
        String odgovor = mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"datum": "%s", "lokacija": "%s",
                                 "mlada": {"ime": "Ana", "prezime": "A", "telefon": "098 1"},
                                 "mladozenja": {"ime": "Ivo", "prezime": "I"},
                                 "dogovorenaCijena": 1500, "dodatakIds": [],
                                 "raspored": [{"vrijeme": "15:00", "naziv": "Crkva", "adresa": "Trg 1"}],
                                 "ekipa": [%s]}
                                """.formatted(datum, lokacija, ekipa)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }
}
