package hr.jbrzica.wedding_manager_api.integracija;

import com.jayway.jsonpath.JsonPath;
import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.repository.EmailKodRepository;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import hr.jbrzica.wedding_manager_api.service.EmailServis;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Zaštite iz sigurnosnog pregleda: ograničenje mailova, enumeracija, linkovi, zaglavlja, refresh u dvije kartice. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SigurnostTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    @Autowired
    EmailKodRepository emailKodRepository;

    @MockitoBean
    EmailServis emailServis;

    // Bez granice bi netko preko aplikacije mogao zatrpati tuđi sandučić i dobivati nove pokušaje pogađanja
    @Test
    void naIstiEmailNajvisePetSifriNaSat() throws Exception {
        String email = UUID.randomUUID() + "@test.hr";
        posalji("/api/auth/registracija", """
                {"nazivTvrtke": "Studio", "ime": "Ana", "email": "%s", "lozinka": "lozinka123"}
                """.formatted(email)).andExpect(status().isCreated());
        for (int i = 0; i < 7; i++) {
            posalji("/api/auth/ponovno-posalji-kod", "{\"email\": \"" + email + "\"}").andExpect(status().isOk());
        }
        verify(emailServis, times(5)).posalji(eq(email), anyString(), anyString());
        assertThat(emailKodRepository.countByEmailAndIstjeceAfter(email, Instant.now().minusSeconds(3600))).isEqualTo(5);
    }

    // Bez ispravne šifre odgovor ne smije otkriti ima li email račun
    @Test
    void prihvacanjePozivaSKrivomSifromNeOtkrivaPostojiLiRacun() throws Exception {
        Korisnik postojeci = podaci.noviVlasnik("Studio", "lozinka123");
        String tijelo = """
                {"email": "%s", "kod": "000000", "ime": "Netko", "lozinka": "lozinka123"}
                """;
        posalji("/api/auth/prihvati-poziv", tijelo.formatted(postojeci.getEmail())).andExpect(status().isBadRequest());
        posalji("/api/auth/prihvati-poziv", tijelo.formatted(UUID.randomUUID() + "@test.hr")).andExpect(status().isBadRequest());
    }

    // javascript: link bi se izvršio na klik; adresa bez sheme se dopuni
    @Test
    void linkGalerijeSamoHttp() throws Exception {
        String token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        String vjencanje = """
                {"datum": "2027-06-12", "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                 "dogovorenaCijena": 1000, "dodatakIds": [], "raspored": [], "ekipa": [], "linkGalerije": "%s"}
                """;
        mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(vjencanje.formatted("javascript:alert(1)")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/vjencanja").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(vjencanje.formatted("galerija.hr/ana")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkGalerije").value("https://galerija.hr/ana"));
    }

    // Stranica se ne smije prikazati u tuđem okviru (clickjacking)
    @Test
    void zaglavljaBraneUgradnjuUTudjuStranicu() throws Exception {
        mvc.perform(get("/api/auth/postavke"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Referrer-Policy"));
    }

    // Dvije kartice osvježe u istoj sekundi: druga dobije 401, ali prijava ostaje (nije krađa)
    @Test
    void istovremenoOsvjezavanjeUDvijeKarticeNeOdjavljuje() throws Exception {
        Korisnik k = podaci.noviVlasnik("Studio", "lozinka123");
        String prvi = kolacic(posalji("/api/auth/prijava",
                "{\"email\": \"" + k.getEmail() + "\", \"lozinka\": \"lozinka123\"}").andReturn().getResponse()
                .getHeader("Set-Cookie"));
        String drugi = kolacic(mvc.perform(post("/api/auth/osvjezi").cookie(new Cookie("wm_osvjezi", prvi)))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("Set-Cookie"));

        mvc.perform(post("/api/auth/osvjezi").cookie(new Cookie("wm_osvjezi", prvi))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/osvjezi").cookie(new Cookie("wm_osvjezi", drugi))).andExpect(status().isOk());
    }

    @Test
    void swaggerJeNaServeruIskljucen() throws Exception {
        String odgovor = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        assertThat(odgovor).doesNotContain("openapi");
    }

    private ResultActions posalji(String putanja, String tijelo) throws Exception {
        return mvc.perform(post(putanja).contentType(MediaType.APPLICATION_JSON).content(tijelo));
    }

    private static String kolacic(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    @SuppressWarnings("unused")
    private static String id(String json) {
        return JsonPath.read(json, "$.id").toString();
    }
}
