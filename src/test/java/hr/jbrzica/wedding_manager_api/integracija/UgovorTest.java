package hr.jbrzica.wedding_manager_api.integracija;

import com.jayway.jsonpath.JsonPath;
import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UgovorTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    @Autowired
    KorisnikRepository korisnikRepository;

    private Korisnik vlasnik;
    private String token;
    private long vjencanje;

    @BeforeEach
    void pripremi() throws Exception {
        vlasnik = podaci.noviVlasnik("Foto Test", "lozinka123");
        token = jwtServis.napravi(vlasnik);
        mvc.perform(put("/api/tvrtka").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"naziv": "Foto Test", "adresa": "Glavna 1, Đakovo", "oib": "12345678903",
                                 "telefon": "091 111 2222", "email": "studio@test.hr"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oib").value("12345678903"));

        long paket = stvori("/api/paketi", "{\"naziv\": \"Deluxe\", \"cijena\": 2800}");
        long dron = stvori("/api/dodaci", "{\"naziv\": \"Dron\", \"cijena\": 0}");
        vjencanje = stvori("/api/vjencanja", """
                {"datum": "2027-05-07", "lokacija": "Restoran Test, Osijek",
                 "mlada": {"ime": "Ana", "prezime": "Horvat", "telefon": "098 000 0001", "adresa": "Ulica 1, Đakovo"},
                 "mladozenja": {"ime": "Ivo", "prezime": "Kovač", "adresa": "Ulica 2, Osijek"},
                 "paketId": %d, "dodatakIds": [%d], "dogovorenaCijena": 2800,
                 "raspored": [{"vrijeme": "11:00", "naziv": "Priprema", "adresa": "Kod mlade"},
                              {"vrijeme": "15:00", "naziv": "Crkva", "adresa": "Župna crkva sv. Petra"}],
                 "ekipa": []}
                """.formatted(paket, dron));
        stvori("/api/vjencanja/" + vjencanje + "/uplate", "{\"iznos\": 200, \"datum\": \"2026-11-11\", \"vrsta\": \"KAPARA\"}");
    }

    @Test
    void ugovorSadrziPodatkeVjencanjaITvrtke() throws Exception {
        String tekst = tekst(preuzmi("/api/vjencanja/" + vjencanje + "/ugovor")
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename=\"Ugovor - Ana Horvat i Ivo Kovac - 7.5.2027.docx\"")))
                .andReturn().getResponse().getContentAsByteArray());

        assertThat(tekst)
                .contains("Ana Horvat i Ivo Kovač", "7. 5. 2027.", "Ulica 2, Osijek", "Ulica 1, Đakovo", "098 000 0001")
                .contains("Župna crkva sv. Petra", "Restoran Test, Osijek")
                .contains("Deluxe", "2.800 €", "200 €", "Dron")
                .contains("Foto Test", "Glavna 1, Đakovo", "12345678903", "091 111 2222", "studio@test.hr")
                .doesNotContain("{{");
    }

    // Mladoženja nema telefon: na ugovoru je crta za dopisivanje rukom
    @Test
    void praznoPoljeJeCrta() throws Exception {
        String tekst = tekst(preuzmi("/api/vjencanja/" + vjencanje + "/ugovor").andReturn().getResponse().getContentAsByteArray());
        assertThat(tekst).contains("Kontakt: ____");
    }

    @Test
    void tudaTvrtkaNeMozeDohvatitiUgovor() throws Exception {
        String drugi = jwtServis.napravi(podaci.noviVlasnik("Druga", "lozinka123"));
        mvc.perform(get("/api/vjencanja/" + vjencanje + "/ugovor").header("Authorization", "Bearer " + drugi))
                .andExpect(status().isNotFound());
    }

    // Word često razbije oznaku na više dijelova; i tako mora raditi. Nepoznata oznaka ostaje vidljiva.
    @Test
    void vlastitiPredlozakSRazbijenomOznakom() throws Exception {
        byte[] predlozak;
        try (XWPFDocument d = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph p = d.createParagraph();
            p.createRun().setText("Ugovor za {{nar");
            p.createRun().setText("ucitelj}}, dvorana {{ dvorana }}, {{nepoznato}}.");
            d.write(out);
            predlozak = out.toByteArray();
        }
        mvc.perform(multipart(HttpMethod.PUT, "/api/tvrtka/ugovor/predlozak")
                        .file(new MockMultipartFile("datoteka", "moj-ugovor.docx", "application/octet-stream", predlozak))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vlastiti").value(true));

        String tekst = tekst(preuzmi("/api/vjencanja/" + vjencanje + "/ugovor").andReturn().getResponse().getContentAsByteArray());
        assertThat(tekst.trim()).isEqualTo("Ugovor za Ana Horvat i Ivo Kovač, dvorana Restoran Test, Osijek, {{nepoznato}}.");

        mvc.perform(delete("/api/tvrtka/ugovor/predlozak").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.vlastiti").value(false));
        assertThat(tekst(preuzmi("/api/vjencanja/" + vjencanje + "/ugovor").andReturn().getResponse().getContentAsByteArray()))
                .contains("UGOVOR O FOTOGRAFIRANJU");
    }

    @Test
    void losaDatotekaSeOdbija() throws Exception {
        posalji("ugovor.docx", "ovo nije word".getBytes()).andExpect(status().isBadRequest());
        posalji("ugovor.pdf", new byte[]{1, 2, 3}).andExpect(status().isBadRequest());
        posalji("veliki.docx", new byte[2 * 1024 * 1024 + 1]).andExpect(status().isBadRequest());
    }

    @Test
    void samoVlasnikMijenjaPredlozak() throws Exception {
        Korisnik clan = new Korisnik();
        clan.setIme("Član");
        clan.setEmail(UUID.randomUUID() + "@test.hr");
        clan.setLozinkaHash("x");
        clan.setUloga(UlogaKorisnika.CLAN);
        clan.setEmailPotvrden(true);
        clan.setTvrtka(vlasnik.getTvrtka());
        String tokenClana = jwtServis.napravi(korisnikRepository.save(clan));

        mvc.perform(multipart(HttpMethod.PUT, "/api/tvrtka/ugovor/predlozak")
                        .file(new MockMultipartFile("datoteka", "x.docx", "application/octet-stream", new byte[]{1}))
                        .header("Authorization", "Bearer " + tokenClana))
                .andExpect(status().isForbidden());
        // Član ugovor smije napraviti
        mvc.perform(get("/api/vjencanja/" + vjencanje + "/ugovor").header("Authorization", "Bearer " + tokenClana))
                .andExpect(status().isOk());
    }

    private ResultActions posalji(String naziv, byte[] sadrzaj) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/tvrtka/ugovor/predlozak")
                .file(new MockMultipartFile("datoteka", naziv, "application/octet-stream", sadrzaj))
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions preuzmi(String putanja) throws Exception {
        return mvc.perform(get(putanja).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    private static String tekst(byte[] docx) throws Exception {
        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor izvlacenje = new XWPFWordExtractor(d)) {
            return izvlacenje.getText();
        }
    }

    private long stvori(String putanja, String tijelo) throws Exception {
        String odgovor = mvc.perform(post(putanja).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(tijelo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }
}
