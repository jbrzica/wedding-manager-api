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
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    // Manje od 24 h prije termina klijent više ne može sam otkazati; studio može uvijek
    @Test
    void manjeOd24SataPrijeSeNeMozeOtkazatiLinkom() throws Exception {
        String sutra = LocalDate.now(java.time.ZoneId.of("Europe/Zagreb")).plusDays(1).toString();
        String odgovor = fotograf(post("/api/termini"), dogadjaj("00:00", "01:00", 30).replace(dan, sutra))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String sutraKod = JsonPath.read(odgovor, "$.javniKod");
        long paket = ((Number) JsonPath.read(odgovor, "$.paketi[0].id")).longValue();
        mvc.perform(get("/api/javno/termini/" + sutraKod)).andExpect(jsonPath("$.otkazSati").value(24));

        mvc.perform(post("/api/javno/termini/" + sutraKod + "/rezervacije").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paketId": %d, "datum": "%s", "vrijeme": "00:00", "ime": "Ana Horvat", "telefon": "091 111 2222",
                         "email": "kasno@test.hr", "privola": true}
                        """.formatted(paket, sutra)))
                .andExpect(status().isCreated());
        ArgumentCaptor<String> tekst = ArgumentCaptor.forClass(String.class);
        verify(emailServis).posalji(eq("kasno@test.hr"), anyString(), tekst.capture());
        assertThat(tekst.getValue()).contains("najkasnije 24 sata prije");
        String otkazniKod = tekst.getValue().replaceAll("(?s).*/rezervacija/otkazi/(\\S+).*", "$1");

        mvc.perform(get("/api/javno/otkazivanje/" + otkazniKod))
                .andExpect(jsonPath("$.moguceOtkazati").value(false))
                .andExpect(jsonPath("$.terminProsao").value(false));
        mvc.perform(post("/api/javno/otkazivanje/" + otkazniKod))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("24 sata")));
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

    @Test
    void naslovnaSlikaSeSpremaIVidiJavno() throws Exception {
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, 5};
        String drugi = jwtServis.napravi(podaci.noviVlasnik("Druga", "lozinka123"));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/termini/" + id + "/slika")
                        .file(new MockMultipartFile("slika", "a.jpg", "image/jpeg", jpg))
                        .header("Authorization", "Bearer " + drugi))
                .andExpect(status().isNotFound());
        // Tekst preimenovan u .jpg se odbija jer se gleda sadržaj
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/termini/" + id + "/slika")
                .file(new MockMultipartFile("slika", "a.jpg", "image/jpeg", "<html>".getBytes()))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/javno/termini/" + kod + "/slika")).andExpect(status().isNotFound());

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/termini/" + id + "/slika")
                .file(new MockMultipartFile("slika", "a.jpg", "image/jpeg", jpg))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slikaVerzija").value(1));
        mvc.perform(get("/api/javno/termini/" + kod)).andExpect(jsonPath("$.slikaVerzija").value(1));
        mvc.perform(get("/api/javno/termini/" + kod + "/slika"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpg));

        fotograf(delete("/api/termini/" + id + "/slika"), null).andExpect(status().isNoContent());
        mvc.perform(get("/api/javno/termini/" + kod + "/slika")).andExpect(status().isNotFound());
        fotograf(get("/api/termini/" + id), null).andExpect(jsonPath("$.slikaVerzija").value(0));
    }

    // Zauzeta vremena se javno vide precrtana, ali bez ikakvih podataka o klijentu
    @Test
    void javnoSeVideZauzetaVremenaBezImena() throws Exception {
        rezerviraj(mini, "10:30", "ana@test.hr").andExpect(status().isCreated());
        mvc.perform(get("/api/javno/termini/" + kod))
                .andExpect(jsonPath("$.dani[0].zauzeto", hasSize(1)))
                .andExpect(jsonPath("$.dani[0].zauzeto[0]").value("10:30:00"))
                .andExpect(jsonPath("$.dani[0].slobodno", hasSize(3)))
                .andExpect(content().string(not(containsString("ana@test.hr"))));
    }

    // Viber/WhatsApp čitaju samo HTML sa servera, pa server sam upiše naslov, opis i sliku
    @Test
    void linkImaPregledZaViberIWhatsApp() throws Exception {
        mvc.perform(get("/rezervacija/" + kod))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta property=\"og:title\" content=\"Božićno fotografiranje\">")))
                .andExpect(content().string(containsString("Foto Test · ")))
                .andExpect(content().string(containsString("od 100 €")))
                .andExpect(content().string(not(containsString("<title>Wedding Manager</title>"))))
                .andExpect(content().string(not(containsString("og:image"))));

        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, 5};
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/termini/" + id + "/slika")
                .file(new MockMultipartFile("slika", "a.jpg", "image/jpeg", jpg))
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/rezervacija/" + kod))
                .andExpect(content().string(containsString("/api/javno/termini/" + kod + "/slika?v=1\">")));

        // Naziv upisuje korisnik: ne smije završiti kao HTML na stranici
        fotograf(put("/api/termini/" + id), dogadjaj("10:00", "12:00", 30)
                .replace("Božićno fotografiranje", "Zlo \\\"><script>alert(1)</script>")).andExpect(status().isOk());
        mvc.perform(get("/rezervacija/" + kod))
                .andExpect(content().string(not(containsString("<script>alert"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));

        // Nepoznat kod: obična aplikacija, koja sama pokaže da link ne vrijedi
        mvc.perform(get("/rezervacija/izmisljeno"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Wedding Manager</title>")));
    }

    @Test
    void kratkiLinkIPoddomenaVodeNaRezervaciju() throws Exception {
        String sKratkim = dogadjaj("10:00", "12:00", 30).replace("\"otvoren\": true", "\"otvoren\": true, \"kratkiLink\": \"bozicno\"");
        fotograf(put("/api/termini/" + id), sKratkim).andExpect(status().isOk())
                .andExpect(jsonPath("$.kratkiLink").value("bozicno"));

        mvc.perform(get("/r/bozicno"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/rezervacija/" + kod));
        mvc.perform(get("/r/nepostoji")).andExpect(header().string("Location", "/"));

        // Viber/WhatsApp odmah dobiju karticu sa slikom, bez preusmjeravanja
        mvc.perform(get("/r/bozicno").header("User-Agent", "WhatsApp/2.23.20.0 A"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("og:title")));
        mvc.perform(get("/").header("User-Agent", "Viber/21.0").with(r -> { r.setServerName("bozicno.brzicaweddings.com"); return r; }))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("og:title")));

        // Poddomena: svi dobiju karticu i preglednik se prebaci na javnu adresu iz postavki (ne na adresu iz zahtjeva).
        // WhatsApp na računalu ne prati preusmjeravanje na drugu domenu, pa 302 ovdje ne bi pokazao karticu.
        mvc.perform(get("/").with(r -> { r.setServerName("bozicno.brzicaweddings.com"); return r; }))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("og:title")))
                .andExpect(content().string(containsString("http-equiv=\"refresh\" content=\"0;url=https://app.brzicaweddings.com/rezervacija/" + kod)))
                .andExpect(content().string(containsString("location.replace(\"https://app.brzicaweddings.com/rezervacija/" + kod + "\")")));
        mvc.perform(get("/").with(r -> { r.setServerName("nepoznato.brzicaweddings.com"); return r; }))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.not(containsString("rezervacija"))));

        // Isti kratki link ne može imati druga tvrtka, ni rezervirane riječi
        String drugi = jwtServis.napravi(podaci.noviVlasnik("Druga", "lozinka123"));
        mvc.perform(post("/api/termini").header("Authorization", "Bearer " + drugi)
                .contentType(MediaType.APPLICATION_JSON).content(sKratkim)).andExpect(status().isConflict());
        fotograf(put("/api/termini/" + id), sKratkim.replace("bozicno", "app")).andExpect(status().isBadRequest());
        fotograf(put("/api/termini/" + id), sKratkim.replace("bozicno", "Božićno")).andExpect(status().isBadRequest());
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
