package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.PrognozaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vremenska prognoza s Open-Meteo (besplatno, bez ključa) za vjenčanja idućih 7 dana.
 * Mjesto se traži iz lokacije: prvo dio iza zareza („Restoran Rubin, Đakovo“ → Đakovo),
 * pa riječi naziva („Hotel Osijek“ → Osijek). Ako servis ne odgovori, prognoze jednostavno nema.
 */
@Service
public class PrognozaServis {

    static final int DANA_UNAPRIJED = 7;
    private static final Logger log = LoggerFactory.getLogger(PrognozaServis.class);
    private static final ZoneId ZAGREB = ZoneId.of("Europe/Zagreb");
    private static final Duration TRAJANJE_PROGNOZE = Duration.ofHours(1);
    // Riječi koje su vrsta mjesta, a ne ime grada
    private static final Set<String> NISU_MJESTA = Set.of("hotel", "restoran", "dvorac", "vila", "villa", "sala",
            "crkva", "kuća", "kuca", "imanje", "salon", "dvorana", "konoba", "kod", "centar", "park", "vinarija");

    private record Koordinate(String mjesto, double lat, double lon) {
    }

    private record Spremljena(PrognozaResponse prognoza, Instant vrijeme) {
    }

    private final boolean ukljucena;
    private final RestClient geokodiranje;
    private final RestClient prognoze;
    // Prazan Optional = mjesto nije pronađeno, da se ne traži svaki put iznova
    private final Map<String, Optional<Koordinate>> mjesta = new ConcurrentHashMap<>();
    private final Map<String, Spremljena> spremljene = new ConcurrentHashMap<>();

    public PrognozaServis(@Value("${app.prognoza.ukljucena:true}") boolean ukljucena) {
        this.ukljucena = ukljucena;
        SimpleClientHttpRequestFactory tvornica = new SimpleClientHttpRequestFactory();
        tvornica.setConnectTimeout(Duration.ofSeconds(3));
        tvornica.setReadTimeout(Duration.ofSeconds(3));
        this.geokodiranje = RestClient.builder().requestFactory(tvornica).baseUrl("https://geocoding-api.open-meteo.com/v1").build();
        this.prognoze = RestClient.builder().requestFactory(tvornica).baseUrl("https://api.open-meteo.com/v1").build();
    }

    public static boolean uRasponu(LocalDate datum) {
        LocalDate danas = LocalDate.now(ZAGREB);
        return !datum.isBefore(danas) && !datum.isAfter(danas.plusDays(DANA_UNAPRIJED));
    }

    public Optional<PrognozaResponse> za(String lokacija, LocalDate datum) {
        if (!ukljucena || lokacija == null || lokacija.isBlank() || !uRasponu(datum)) {
            return Optional.empty();
        }
        try {
            return koordinate(lokacija).flatMap(k -> prognoza(k, datum));
        } catch (RuntimeException e) {
            log.warn("Prognoza nije dostupna za {}: {}", lokacija, e.getMessage());
            return Optional.empty();
        }
    }

    // Mogući nazivi mjesta iz lokacije, od najvjerojatnijeg
    static List<String> kandidati(String lokacija) {
        List<String> kandidati = new ArrayList<>();
        String[] dijelovi = lokacija.split(",");
        for (int i = dijelovi.length - 1; i >= 1; i--) {
            String d = dijelovi[i].replaceAll("\\d", "").trim();
            if (d.length() >= 3) {
                kandidati.add(d);
            }
        }
        String[] rijeci = dijelovi[0].trim().split("\\s+");
        for (int i = rijeci.length - 1; i >= 0; i--) {
            String r = rijeci[i].replaceAll("[^\\p{L}-]", "");
            if (r.length() >= 3 && !NISU_MJESTA.contains(r.toLowerCase(Locale.ROOT))) {
                kandidati.add(r);
            }
        }
        return kandidati;
    }

    private Optional<Koordinate> koordinate(String lokacija) {
        for (String kandidat : kandidati(lokacija)) {
            Optional<Koordinate> k = mjesta.computeIfAbsent(kandidat.toLowerCase(Locale.ROOT), x -> trazi(kandidat));
            if (k.isPresent()) {
                return k;
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Optional<Koordinate> trazi(String naziv) {
        Map<String, Object> odgovor = geokodiranje.get()
                .uri(u -> u.path("/search").queryParam("name", naziv).queryParam("count", 1)
                        .queryParam("language", "hr").queryParam("countryCode", "HR").build())
                .retrieve().body(Map.class);
        List<Map<String, Object>> rezultati = odgovor == null ? null : (List<Map<String, Object>>) odgovor.get("results");
        if (rezultati == null || rezultati.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> r = rezultati.get(0);
        return Optional.of(new Koordinate((String) r.get("name"),
                ((Number) r.get("latitude")).doubleValue(), ((Number) r.get("longitude")).doubleValue()));
    }

    @SuppressWarnings("unchecked")
    private Optional<PrognozaResponse> prognoza(Koordinate k, LocalDate datum) {
        String kljuc = k.lat() + "," + k.lon() + "," + datum;
        Spremljena s = spremljene.get(kljuc);
        if (s != null && s.vrijeme().plus(TRAJANJE_PROGNOZE).isAfter(Instant.now())) {
            return Optional.of(s.prognoza());
        }
        Map<String, Object> odgovor = prognoze.get()
                .uri(u -> u.path("/forecast").queryParam("latitude", k.lat()).queryParam("longitude", k.lon())
                        .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                        .queryParam("timezone", "Europe/Zagreb")
                        .queryParam("start_date", datum).queryParam("end_date", datum).build())
                .retrieve().body(Map.class);
        Map<String, List<Object>> dnevno = odgovor == null ? null : (Map<String, List<Object>>) odgovor.get("daily");
        if (dnevno == null || dnevno.get("weather_code") == null || dnevno.get("weather_code").isEmpty()) {
            return Optional.empty();
        }
        Object oborine = dnevno.get("precipitation_probability_max") == null ? null : dnevno.get("precipitation_probability_max").get(0);
        PrognozaResponse p = new PrognozaResponse(datum, k.mjesto(),
                ((Number) dnevno.get("weather_code").get(0)).intValue(),
                ((Number) dnevno.get("temperature_2m_max").get(0)).doubleValue(),
                ((Number) dnevno.get("temperature_2m_min").get(0)).doubleValue(),
                oborine == null ? null : ((Number) oborine).intValue());
        spremljene.put(kljuc, new Spremljena(p, Instant.now()));
        return Optional.of(p);
    }
}
