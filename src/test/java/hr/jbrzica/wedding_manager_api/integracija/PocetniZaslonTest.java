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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PocetniZaslonTest {

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

    // Upit usred razgovora: dovoljni su datum i ime; sve ostalo se dopuni kasnije
    @Test
    void brziUpitSpremaSeKaoUpit() throws Exception {
        mvc.perform(post("/api/vjencanja/upit").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"datum\": \"2027-09-11\", \"ime\": \" Ana Marija  Horvat \", \"telefon\": \"098 111 222\", \"napomena\": \"Zove sutra\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPIT"))
                .andExpect(jsonPath("$.mlada.ime").value("Ana"))
                .andExpect(jsonPath("$.mlada.prezime").value("Marija  Horvat"))
                .andExpect(jsonPath("$.mlada.telefon").value("098 111 222"))
                .andExpect(jsonPath("$.mladozenja.ime").value(""))
                .andExpect(jsonPath("$.napomene").value("Zove sutra"))
                .andExpect(jsonPath("$.zadaci", hasSize(5)));

        mvc.perform(get("/api/vjencanja").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[0].mlada").value("Ana Marija  Horvat"))
                .andExpect(jsonPath("$.content[0].mladozenja").value(""));
    }

    @Test
    void brziUpitBezImenaIliDatumaSeOdbija() throws Exception {
        mvc.perform(post("/api/vjencanja/upit").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"datum\": \"2027-09-11\", \"ime\": \" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/vjencanja/upit").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ime\": \"Ana\"}"))
                .andExpect(status().isBadRequest());
    }

    // Brojevi na početnom zaslonu moraju odgovarati onome što filter otvori
    @Test
    void pregledBrojiStoTrebaPaznju() throws Exception {
        long paket = stvori("/api/paketi", "{\"naziv\": \"Classic\", \"cijena\": 2600}");
        String sutra = LocalDate.now().plusDays(1).toString();

        // Potvrđeno s kaparom, s rasporedom: ovo je sljedeće vjenčanje
        long sKaparom = novo(sutra, null, paket, "[{\"vrijeme\": \"13:30\", \"naziv\": \"Crkva\"}, {\"vrijeme\": \"11:00\", \"naziv\": \"Priprema\"}]");
        postaviStatus(sKaparom, "POTVRDENO");
        stvori("/api/vjencanja/" + sKaparom + "/uplate", "{\"iznos\": 200, \"datum\": \"2026-11-06\", \"vrsta\": \"KAPARA\"}");

        // Potvrđeno, uplaćena samo rata: i dalje bez kapare
        long bezKapare = novo(LocalDate.now().plusDays(30).toString(), null, null, "[]");
        postaviStatus(bezKapare, "POTVRDENO");
        stvori("/api/vjencanja/" + bezKapare + "/uplate", "{\"iznos\": 100, \"datum\": \"2026-11-06\", \"vrsta\": \"RATA\"}");

        // Upit koji tek dolazi i upit koji je prošao (taj se ne broji)
        novo(LocalDate.now().plusDays(60).toString(), null, null, "[]");
        novo(LocalDate.now().minusDays(60).toString(), null, null, "[]");

        // Odrađeno, rok isporuke prošao
        long kasni = novo(LocalDate.now().minusMonths(3).toString(), LocalDate.now().minusDays(1).toString(), null, "[]");
        postaviStatus(kasni, "ODRADENO");

        mvc.perform(get("/api/vjencanja/pregled").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.sljedeceVjencanje.id").value(sKaparom))
                .andExpect(jsonPath("$.sljedeceVjencanje.paket").value("Classic"))
                .andExpect(jsonPath("$.danaDoSljedeceg").value(1))
                .andExpect(jsonPath("$.pocetakSljedeceg").value("11:00:00"))
                .andExpect(jsonPath("$.ekipaSljedeceg").value(0))
                .andExpect(jsonPath("$.kasniIsporuka").value(1))
                .andExpect(jsonPath("$.bezKapare").value(1))
                .andExpect(jsonPath("$.noviUpiti").value(1));

        mvc.perform(get("/api/vjencanja?bezKapare=true").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(bezKapare))
                .andExpect(jsonPath("$.content[0].paket").doesNotExist());
    }

    private void postaviStatus(long id, String status) throws Exception {
        mvc.perform(patch("/api/vjencanja/" + id + "/status?status=" + status).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private long novo(String datum, String rokIsporuke, Long paketId, String raspored) throws Exception {
        String rok = rokIsporuke == null ? "null" : "\"" + rokIsporuke + "\"";
        return stvori("/api/vjencanja", """
                {"datum": "%s", "rokIsporuke": %s, "paketId": %s,
                 "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                 "dogovorenaCijena": 1000, "dodatakIds": [], "raspored": %s, "ekipa": []}
                """.formatted(datum, rok, paketId, raspored));
    }

    private long stvori(String putanja, String tijelo) throws Exception {
        String odgovor = mvc.perform(post(putanja).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(tijelo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }
}
