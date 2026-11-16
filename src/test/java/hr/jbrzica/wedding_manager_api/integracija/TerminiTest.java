package hr.jbrzica.wedding_manager_api.integracija;

import com.jayway.jsonpath.JsonPath;
import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import hr.jbrzica.wedding_manager_api.service.EmailServis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Termini za kratka fotografiranja: fotograf postavi dane i pakete, klijenti rezerviraju preko javnog linka. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TerminiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    @MockitoBean
    EmailServis emailServis;

    private final String dan = LocalDate.now().plusDays(30).toString();
    private String token;
    private long id;
    private String kod;
    private long mini;
    private long standard;

    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Foto Test", "lozinka123"));
        String odgovor = fotograf(post("/api/termini"), dogadjaj("10:00", "12:00", 30))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ukupnoTermina").value(4))
                .andReturn().getResponse().getContentAsString();
        id = ((Number) JsonPath.read(odgovor, "$.id")).longValue();
        kod = JsonPath.read(odgovor, "$.javniKod");
        mini = ((Number) JsonPath.read(odgovor, "$.paketi[0].id")).longValue();
        standard = ((Number) JsonPath.read(odgovor, "$.paketi[1].id")).longValue();
    }

    @Test
    void klijentRezerviraIDobijePotvrdu() throws Exception {
        mvc.perform(get("/api/javno/termini/" + kod))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paketi", hasSize(2)))
                .andExpect(jsonPath("$.dani[0].slobodno", hasSize(4)))
                .andExpect(jsonPath("$.dani[0].slobodno[0]").value("10:00:00"))
                .andExpect(jsonPath("$.dani[0].slobodno[3]").value("11:30:00"));

        rezerviraj(standard, "10:30", "ana@test.hr")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nazivPaketa").value("Standard"))
                .andExpect(jsonPath("$.cijena").value(120));

        // Javno se vidi samo da je zauzeto, ne i tko je rezervirao
        mvc.perform(get("/api/javno/termini/" + kod))
                .andExpect(jsonPath("$.dani[0].slobodno", hasSize(3)))
                .andExpect(content().string(not(containsString("Ana"))));
        fotograf(get("/api/termini/" + id), null)
                .andExpect(jsonPath("$.zauzeto").value(1))
                .andExpect(jsonPath("$.prihod").value(120))
                .andExpect(jsonPath("$.raspored[0].mjesta[1].rezervacija.ime").value("Ana Horvat"))
                .andExpect(jsonPath("$.raspored[0].mjesta[0].rezervacija").doesNotExist());
        verify(emailServis).posalji(eq("ana@test.hr"), anyString(), anyString());
    }

    // Jedinstveni indeks u bazi: isti termin dvaput ne može, ni ako se klikne u istoj sekundi
    @Test
    void zauzetiTerminSeNeMozeRezerviratiDvaput() throws Exception {
        rezerviraj(mini, "11:00", "prvi@test.hr").andExpect(status().isCreated());
        rezerviraj(standard, "11:00", "drugi@test.hr").andExpect(status().isConflict());
    }

    @Test
    void vrijemeIzvanRasporedaIProsliDanSeOdbijaju() throws Exception {
        rezerviraj(mini, "10:15", "a@test.hr").andExpect(status().isBadRequest());
        rezerviraj(mini, "12:00", "a@test.hr").andExpect(status().isBadRequest());
    }

    @Test
    void najviseDvaTerminaNaIstiEmail() throws Exception {
        rezerviraj(mini, "10:00", "isti@test.hr").andExpect(status().isCreated());
        rezerviraj(mini, "10:30", "ISTI@test.hr").andExpect(status().isCreated());
        rezerviraj(mini, "11:00", "isti@test.hr").andExpect(status().isConflict());
    }

    @Test
    void otkazivanjeLinkomIzMailaOslobodiTermin() throws Exception {
        rezerviraj(mini, "10:00", "ana@test.hr").andExpect(status().isCreated());
        ArgumentCaptor<String> tekst = ArgumentCaptor.forClass(String.class);
        verify(emailServis).posalji(eq("ana@test.hr"), anyString(), tekst.capture());
        String otkazniKod = tekst.getValue().replaceAll("(?s).*/rezervacija/otkazi/(\\S+).*", "$1");

        mvc.perform(get("/api/javno/otkazivanje/" + otkazniKod))
                .andExpect(jsonPath("$.moguceOtkazati").value(true));
        mvc.perform(post("/api/javno/otkazivanje/" + otkazniKod))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otkazano").value(true));
        mvc.perform(get("/api/javno/termini/" + kod)).andExpect(jsonPath("$.dani[0].slobodno", hasSize(4)));
        mvc.perform(get("/api/javno/otkazivanje/izmisljeno")).andExpect(status().isNotFound());
    }

    @Test
    void zatvorenDogadjajIStariLinkNeRade() throws Exception {
        fotograf(put("/api/termini/" + id), dogadjaj("10:00", "12:00", 30).replace("\"otvoren\": true", "\"otvoren\": false"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/javno/termini/" + kod)).andExpect(jsonPath("$.dani[0].slobodno", hasSize(0)));
        rezerviraj(mini, "10:00", "a@test.hr").andExpect(status().isConflict());

        fotograf(post("/api/termini/" + id + "/novi-link"), null).andExpect(status().isOk());
        mvc.perform(get("/api/javno/termini/" + kod)).andExpect(status().isNotFound());
    }

    // Promjena sati ne smije ostaviti nečiju rezervaciju izvan rasporeda
    @Test
    void satiSeNeMoguSkratitiPrekoRezervacije() throws Exception {
        rezerviraj(mini, "11:30", "a@test.hr").andExpect(status().isCreated());
        fotograf(put("/api/termini/" + id), dogadjaj("10:00", "11:00", 30)).andExpect(status().isConflict());
        fotograf(put("/api/termini/" + id), dogadjaj("09:00", "13:00", 30)).andExpect(status().isOk());
    }

    @Test
    void paketDuljiOdRazmakaSeOdbija() throws Exception {
        fotograf(post("/api/termini"), dogadjaj("10:00", "12:00", 20)).andExpect(status().isBadRequest());
    }

    @Test
    void rucnaRezervacijaIOtkazivanjeUAplikaciji() throws Exception {
        String odgovor = fotograf(post("/api/termini/" + id + "/rezervacije"), """
                {"paketId": %d, "datum": "%s", "vrijeme": "10:00", "ime": "Marko (nazvao)"}
                """.formatted(mini, dan)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long rezervacija = ((Number) JsonPath.read(odgovor, "$.id")).longValue();
        rezerviraj(mini, "10:00", "a@test.hr").andExpect(status().isConflict());

        fotograf(post("/api/termini/rezervacije/" + rezervacija + "/otkazi"), null).andExpect(status().isNoContent());
        rezerviraj(mini, "10:00", "a@test.hr").andExpect(status().isCreated());
    }

    @Test
    void tudaTvrtkaNeVidiTermine() throws Exception {
        String drugi = jwtServis.napravi(podaci.noviVlasnik("Druga", "lozinka123"));
        mvc.perform(get("/api/termini/" + id).header("Authorization", "Bearer " + drugi)).andExpect(status().isNotFound());
        String popis = mvc.perform(get("/api/termini").header("Authorization", "Bearer " + drugi))
                .andReturn().getResponse().getContentAsString();
        assertThat((List<?>) JsonPath.read(popis, "$")).isEmpty();
        mvc.perform(get("/api/termini/" + id)).andExpect(status().isUnauthorized());
    }

    private String dogadjaj(String od, String doVremena, int korak) {
        return """
                {"naziv": "Božićno fotografiranje", "lokacija": "Studio", "korakMinuta": %d, "otvoren": true,
                 "paketi": [{"naziv": "Mini", "trajanjeMinuta": 20, "cijena": 100},
                            {"naziv": "Standard", "trajanjeMinuta": 30, "cijena": 120}],
                 "dani": [{"datum": "%s", "od": "%s", "doVremena": "%s"}]}
                """.formatted(korak, dan, od, doVremena);
    }

    private ResultActions rezerviraj(long paket, String vrijeme, String email) throws Exception {
        return mvc.perform(post("/api/javno/termini/" + kod + "/rezervacije").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paketId": %d, "datum": "%s", "vrijeme": "%s", "ime": "Ana Horvat", "telefon": "091 111 2222",
                         "email": "%s", "privola": true}
                        """.formatted(paket, dan, vrijeme, email)));
    }

    private ResultActions fotograf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder zahtjev,
                                   String tijelo) throws Exception {
        zahtjev.header("Authorization", "Bearer " + token);
        if (tijelo != null) {
            zahtjev.contentType(MediaType.APPLICATION_JSON).content(tijelo);
        }
        return mvc.perform(zahtjev);
    }
}
