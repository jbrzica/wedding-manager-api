package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.KalendarLinkResponse;
import hr.jbrzica.wedding_manager_api.entity.Osoba;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.StavkaRasporeda;
import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
import hr.jbrzica.wedding_manager_api.security.TajniToken;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Vjenčanja kao iCal (.ics) kalendar na koji se korisnik pretplati u Google ili Apple kalendaru.
 * Kalendarske aplikacije ne mogu poslati JWT, pa se tvrtka prepoznaje po tajnom tokenu u linku.
 */
@Service
public class KalendarService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter VRIJEME_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private final TvrtkaRepository tvrtkaRepository;
    private final VjencanjeRepository vjencanjeRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public KalendarService(TvrtkaRepository tvrtkaRepository, VjencanjeRepository vjencanjeRepository,
                           TrenutniKorisnik trenutniKorisnik) {
        this.tvrtkaRepository = tvrtkaRepository;
        this.vjencanjeRepository = vjencanjeRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @Transactional
    public KalendarLinkResponse link() {
        Tvrtka tvrtka = mojaTvrtka();
        if (tvrtka.getKalendarToken() == null) {
            tvrtka.setKalendarToken(TajniToken.novi());
        }
        return KalendarLinkResponse.za(tvrtka.getKalendarToken());
    }

    // Novi token odmah poništava stari link, npr. ako je slučajno poslan pogrešnoj osobi.
    @Transactional
    public KalendarLinkResponse noviLink() {
        if (trenutniKorisnik.get().uloga() != UlogaKorisnika.VLASNIK) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ovo može samo vlasnik tvrtke");
        }
        Tvrtka tvrtka = mojaTvrtka();
        tvrtka.setKalendarToken(TajniToken.novi());
        return KalendarLinkResponse.za(tvrtka.getKalendarToken());
    }

    @Transactional(readOnly = true)
    public String ical(String token) {
        Tvrtka tvrtka = tvrtkaRepository.findByKalendarToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Vjencanje> vjencanja = vjencanjeRepository.findByTvrtkaIdAndStatusNotIn(
                tvrtka.getId(), Set.of(StatusVjencanja.OTKAZANO));
        return izgradi("Vjenčanja – " + tvrtka.getNaziv(), vjencanja);
    }

    // Isti format koristi i osobni kalendar suradnika, samo s njegovim vjenčanjima.
    public String izgradi(String nazivKalendara, List<Vjencanje> vjencanja) {
        List<String> redovi = new ArrayList<>();
        redovi.add("BEGIN:VCALENDAR");
        redovi.add("VERSION:2.0");
        redovi.add("PRODID:-//Wedding Manager//HR");
        redovi.add("CALSCALE:GREGORIAN");
        redovi.add("METHOD:PUBLISH");
        redovi.add("X-WR-CALNAME:" + tekst(nazivKalendara));
        redovi.add("X-WR-TIMEZONE:Europe/Zagreb");
        String sada = VRIJEME_UTC.format(Instant.now());
        for (Vjencanje v : vjencanja) {
            dodajDogadaj(redovi, v, sada);
        }
        redovi.add("END:VCALENDAR");

        StringBuilder sb = new StringBuilder();
        for (String red : redovi) {
            sb.append(presavij(red)).append("\r\n");
        }
        return sb.toString();
    }

    // Cjelodnevni događaj: vremena idu u opis, pa nema problema s vremenskim zonama.
    private void dodajDogadaj(List<String> redovi, Vjencanje v, String sada) {
        LocalDate datum = v.getDatum();
        String naslov = "Vjenčanje: " + imena(v) + (v.getStatus() == StatusVjencanja.UPIT ? " (upit)" : "");

        redovi.add("BEGIN:VEVENT");
        redovi.add("UID:vjencanje-" + v.getId() + "@wedding-manager");
        redovi.add("DTSTAMP:" + sada);
        redovi.add("DTSTART;VALUE=DATE:" + DATUM.format(datum));
        redovi.add("DTEND;VALUE=DATE:" + DATUM.format(datum.plusDays(1)));
        redovi.add("SUMMARY:" + tekst(naslov));
        if (v.getLokacija() != null && !v.getLokacija().isBlank()) {
            redovi.add("LOCATION:" + tekst(v.getLokacija()));
        }
        redovi.add("DESCRIPTION:" + tekst(opis(v)));
        redovi.add("TRANSP:TRANSPARENT");
        redovi.add("END:VEVENT");
    }

    private String opis(Vjencanje v) {
        StringBuilder sb = new StringBuilder();
        for (StavkaRasporeda s : v.getRaspored()) {
            sb.append(s.getVrijeme() != null ? s.getVrijeme().toString() : "--:--")
                    .append(' ').append(s.getNaziv());
            if (s.getAdresa() != null && !s.getAdresa().isBlank()) {
                sb.append(" – ").append(s.getAdresa());
            }
            sb.append('\n');
        }
        dodajKontakt(sb, "Mlada", v.getMlada());
        dodajKontakt(sb, "Mladoženja", v.getMladozenja());
        return sb.toString().trim();
    }

    private void dodajKontakt(StringBuilder sb, String uloga, Osoba o) {
        if (o != null && o.getTelefon() != null && !o.getTelefon().isBlank()) {
            sb.append('\n').append(uloga).append(": ").append(o.punoIme()).append(", ").append(o.getTelefon());
        }
    }

    private String imena(Vjencanje v) {
        String mlada = v.getMlada() != null && v.getMlada().getIme() != null ? v.getMlada().getIme() : "";
        String mladozenja = v.getMladozenja() != null && v.getMladozenja().getIme() != null
                ? v.getMladozenja().getIme() : "";
        return (mlada + " & " + mladozenja).trim();
    }

    // Zarez, točka-zarez i novi red imaju posebno značenje u iCal formatu.
    private static String tekst(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    // Standard traži da red nije dulji od 75 bajtova; nastavak počinje razmakom.
    private static String presavij(String red) {
        StringBuilder sb = new StringBuilder();
        int bajtova = 0;
        for (int i = 0; i < red.length(); ) {
            int znak = red.codePointAt(i);
            String s = new String(Character.toChars(znak));
            int duljina = s.getBytes(StandardCharsets.UTF_8).length;
            if (bajtova + duljina > 75) {
                sb.append("\r\n ");
                bajtova = 1;
            }
            sb.append(s);
            bajtova += duljina;
            i += Character.charCount(znak);
        }
        return sb.toString();
    }

    private Tvrtka mojaTvrtka() {
        return tvrtkaRepository.findById(trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tvrtka ne postoji"));
    }
}
