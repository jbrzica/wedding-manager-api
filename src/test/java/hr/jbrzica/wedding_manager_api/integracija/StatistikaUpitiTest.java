package hr.jbrzica.wedding_manager_api.integracija;

import com.jayway.jsonpath.JsonPath;
import hr.jbrzica.wedding_manager_api.TestcontainersConfiguration;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Statistika mora imati isti broj SQL upita bez obzira na to koliko vjenčanja tvrtka ima.
 * Prije je za svako vjenčanje posebno učitavala uplate, ekipu i dodatke (N+1).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatistikaUpitiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestPodaci podaci;

    @Autowired
    JwtServis jwtServis;

    @Autowired
    EntityManagerFactory emf;

    private String token;
    private long paket;
    private long dron;
    private long album;
    private long marko;
    private long petra;

    @BeforeEach
    void pripremi() throws Exception {
        token = jwtServis.napravi(podaci.noviVlasnik("Studio", "lozinka123"));
        paket = stvori("/api/paketi", "{\"naziv\": \"Cijeli dan\", \"cijena\": 2000}");
        dron = stvori("/api/dodaci", "{\"naziv\": \"Dron\", \"cijena\": 200}");
        album = stvori("/api/dodaci", "{\"naziv\": \"Album\", \"cijena\": 300}");
        marko = stvori("/api/suradnici", "{\"ime\": \"Marko\", \"uloga\": \"VIDEOGRAF\"}");
        petra = stvori("/api/suradnici", "{\"ime\": \"Petra\", \"uloga\": \"FOTOGRAF\"}");
    }

    @Test
    void brojUpitaNeRasteSBrojemVjencanja() throws Exception {
        for (int i = 0; i < 3; i++) {
            potvrdeno(i);
        }
        long uz3 = upitaZaStatistiku();

        for (int i = 3; i < 20; i++) {
            potvrdeno(i);
        }
        long uz20 = upitaZaStatistiku();

        assertThat(uz20).as("broj SQL upita za 20 vjenčanja").isEqualTo(uz3);
        assertThat(uz20).isLessThanOrEqualTo(6);
    }

    // 4 vjenčanja po 2500 € (paket 2000 + dron 200 + album 300), Marko 250 € na svakom, Petra 300 € na dva
    @Test
    void iznosiSuIstiKaoPrije() throws Exception {
        for (int i = 0; i < 4; i++) {
            potvrdeno(i);
        }
        mvc.perform(get("/api/statistika?godina=2027").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ovaGodina.broj").value(4))
                .andExpect(jsonPath("$.ovaGodina.prihod").value(10000.0))
                // 10000 - 4 × 250 (Marko) - 2 × 300 (Petra)
                .andExpect(jsonPath("$.ovaGodina.zarada").value(8400.0))
                .andExpect(jsonPath("$.zaNaplatiti").value(10000 - 4 * 500))
                .andExpect(jsonPath("$.poMjesecima[5].uplaceno").value(4 * 500.0))
                .andExpect(jsonPath("$.paketi[0].naziv").value("Cijeli dan"))
                .andExpect(jsonPath("$.paketi[0].broj").value(4))
                .andExpect(jsonPath("$.dodaci[0].broj").value(4))
                .andExpect(jsonPath("$.dodaci[1].broj").value(4))
                .andExpect(jsonPath("$.honorari[0].ime").value("Marko"))
                .andExpect(jsonPath("$.honorari[0].brojVjencanja").value(4))
                .andExpect(jsonPath("$.honorari[0].ukupno").value(1000.0))
                .andExpect(jsonPath("$.honorari[1].ime").value("Petra"))
                .andExpect(jsonPath("$.honorari[1].brojVjencanja").value(2))
                .andExpect(jsonPath("$.honorari[1].ukupno").value(600.0));
    }

    private long upitaZaStatistiku() throws Exception {
        Statistics statistika = emf.unwrap(SessionFactory.class).getStatistics();
        statistika.clear();
        mvc.perform(get("/api/statistika?godina=2027").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return statistika.getPrepareStatementCount();
    }

    // Lipanj 2027., s paketom, dva dodatka, ekipom i kaparom od 500 €
    private void potvrdeno(int i) throws Exception {
        String ekipa = i % 2 == 0
                ? "[{\"suradnikId\": %d, \"honorar\": 250}, {\"suradnikId\": %d, \"honorar\": 300}]".formatted(marko, petra)
                : "[{\"suradnikId\": %d, \"honorar\": 250}]".formatted(marko);
        long id = stvori("/api/vjencanja", """
                {"datum": "2027-06-%02d", "mlada": {"ime": "Ana", "prezime": "A"}, "mladozenja": {"ime": "Ivo", "prezime": "I"},
                 "paketId": %d, "dodatakIds": [%d, %d], "raspored": [], "ekipa": %s}
                """.formatted(1 + i, paket, dron, album, ekipa));
        mvc.perform(post("/api/vjencanja/" + id + "/uplate").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iznos\": 500, \"datum\": \"2026-11-09\", \"vrsta\": \"KAPARA\"}"))
                .andExpect(status().isCreated());
    }

    private long stvori(String putanja, String tijelo) throws Exception {
        String odgovor = mvc.perform(post(putanja).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(tijelo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(odgovor, "$.id")).longValue();
    }
}
