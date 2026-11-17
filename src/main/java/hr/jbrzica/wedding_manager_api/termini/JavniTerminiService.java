package hr.jbrzica.wedding_manager_api.termini;

import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.security.TajniToken;
import hr.jbrzica.wedding_manager_api.service.EmailServis;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.JavnaRezervacijaRequest;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.JavniDan;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.JavniDogadjaj;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.JavniPaket;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.PotvrdaRezervacije;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Javna stranica za rezervaciju: pristup daje tajni kod u linku, prijava ne treba.
 * Vidi se samo što je slobodno, nikad tko je što rezervirao.
 */
@Service
public class JavniTerminiService {

    // Isti email smije imati najviše toliko aktivnih termina na jednom događaju, da netko ne zauzme sve
    static final int NAJVISE_PO_EMAILU = 2;
    // Klijent sam otkazuje najkasnije toliko sati prije termina; poslije toga zove studio
    static final int OTKAZ_SATI = 24;
    static final ZoneId ZONA = ZoneId.of("Europe/Zagreb");
    private static final Locale HR = Locale.forLanguageTag("hr-HR");
    private static final DateTimeFormatter DAN = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy.", HR);
    private static final Logger log = LoggerFactory.getLogger(JavniTerminiService.class);

    private final DogadjajTerminaRepository dogadjajRepository;
    private final RezervacijaTerminaRepository rezervacijaRepository;
    private final RezervacijaZapis zapis;
    private final EmailServis emailServis;
    private final String javnaAdresa;
    private final Clock sat;

    public JavniTerminiService(DogadjajTerminaRepository dogadjajRepository,
                               RezervacijaTerminaRepository rezervacijaRepository, RezervacijaZapis zapis,
                               EmailServis emailServis, @Value("${app.javna-adresa}") String javnaAdresa) {
        this.dogadjajRepository = dogadjajRepository;
        this.rezervacijaRepository = rezervacijaRepository;
        this.zapis = zapis;
        this.emailServis = emailServis;
        // Adresa za link u mailu dolazi iz postavki, nikad iz zahtjeva (Host zaglavlje se može podmetnuti)
        this.javnaAdresa = javnaAdresa.replaceAll("/+$", "");
        this.sat = Clock.system(ZONA);
    }

    @Transactional(readOnly = true)
    public JavniDogadjaj dogadjaj(String kod) {
        DogadjajTermina d = poKodu(kod);
        Tvrtka t = d.getTvrtka();
        List<JavniPaket> paketi = d.getPaketi().stream()
                .map(p -> new JavniPaket(p.getId(), p.getNaziv(), p.getOpis(), p.getTrajanjeMinuta(), p.getCijena()))
                .toList();
        Set<String> zauzeto = new HashSet<>();
        rezervacijaRepository.findByDogadjajIdAndOtkazanoIsNullOrderByDatumAscVrijemeAsc(d.getId())
                .forEach(r -> zauzeto.add(r.getDatum() + " " + r.getVrijeme()));
        LocalDateTime sada = LocalDateTime.now(sat);
        List<JavniDan> dani = d.getDani().stream().map(DanTermina::getDatum).distinct().sorted()
                .filter(datum -> !datum.isBefore(sada.toLocalDate()))
                .map(datum -> {
                    List<LocalTime> sva = TerminiService.mjesta(d, datum);
                    // Prošla vremena se ne prikazuju; zauzeta se prikazuju precrtana (samo vrijeme, bez imena)
                    List<LocalTime> buduca = sva.stream().filter(v -> datum.atTime(v).isAfter(sada)).toList();
                    List<LocalTime> zauzeta = buduca.stream().filter(v -> zauzeto.contains(datum + " " + v)).toList();
                    List<LocalTime> slobodno = !d.isOtvoren() ? List.<LocalTime>of()
                            : buduca.stream().filter(v -> !zauzeto.contains(datum + " " + v)).toList();
                    return new JavniDan(datum, sva.size(), slobodno, zauzeta);
                }).toList();
        return new JavniDogadjaj(d.getNaziv(), d.getOpis(), d.getLokacija(), t.getNaziv(), t.getTelefon(),
                d.isOtvoren(), d.getKorakMinuta(), OTKAZ_SATI, d.getSlikaVerzija(), paketi, dani);
    }

    @Transactional
    public PotvrdaRezervacije rezerviraj(String kod, JavnaRezervacijaRequest request) {
        if (request.web() != null && !request.web().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rezervacija nije uspjela.");
        }
        DogadjajTermina d = poKodu(kod);
        if (!d.isOtvoren()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rezervacije za ovo fotografiranje su zatvorene.");
        }
        PaketTermina paket = TerminiService.paket(d, request.paketId());
        if (!TerminiService.mjesta(d, request.datum()).contains(request.vrijeme())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taj termin ne postoji. Odaberi drugo vrijeme.");
        }
        if (!request.datum().atTime(request.vrijeme()).isAfter(LocalDateTime.now(sat))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taj termin je već prošao.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (rezervacijaRepository.aktivnihNaEmail(d.getId(), email) >= NAJVISE_PO_EMAILU) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Na ovaj email već su rezervirana " + NAJVISE_PO_EMAILU + " termina. Za više nazovi studio.");
        }

        String otkazniKod = TajniToken.novi();
        RezervacijaTermina r = zapis.spremi(d, paket, request.datum(), request.vrijeme(), request.ime(),
                request.telefon(), email, request.napomena(), false, hash(otkazniKod));
        posaljiPotvrdu(r, otkazniKod);
        obavijestiStudio(r);
        return potvrda(r);
    }

    @Transactional(readOnly = true)
    public PotvrdaRezervacije poOtkaznomKodu(String otkazniKod) {
        return potvrda(poOtkaznom(otkazniKod));
    }

    @Transactional
    public PotvrdaRezervacije otkazi(String otkazniKod) {
        RezervacijaTermina r = poOtkaznom(otkazniKod);
        if (r.getOtkazano() == null) {
            if (!moguceOtkazati(r)) {
                Tvrtka t = r.getDogadjaj().getTvrtka();
                String poruka = terminProsao(r) ? "Termin je već prošao, ne može se otkazati."
                        : "Online otkazivanje je moguće najkasnije " + OTKAZ_SATI + " sata prije termina."
                        + (t.getTelefon() == null ? " Javi se studiju." : " Nazovi studio: " + t.getTelefon());
                throw new ResponseStatusException(HttpStatus.CONFLICT, poruka);
            }
            r.setOtkazano(Instant.now());
            obavijestiStudioOtkazano(r);
        }
        return potvrda(r);
    }

    // ---------- pomoćno ----------

    private DogadjajTermina poKodu(String kod) {
        return dogadjajRepository.findByJavniKod(kod)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Link ne postoji ili više ne vrijedi. Javi se studiju za novi."));
    }

    private RezervacijaTermina poOtkaznom(String otkazniKod) {
        return rezervacijaRepository.findByOtkazniHash(hash(otkazniKod))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rezervacija ne postoji."));
    }

    private boolean moguceOtkazati(RezervacijaTermina r) {
        return r.getOtkazano() == null && rokOtkaza(r).isAfter(LocalDateTime.now(sat));
    }

    private boolean terminProsao(RezervacijaTermina r) {
        return !r.getDatum().atTime(r.getVrijeme()).isAfter(LocalDateTime.now(sat));
    }

    private static LocalDateTime rokOtkaza(RezervacijaTermina r) {
        return r.getDatum().atTime(r.getVrijeme()).minusHours(OTKAZ_SATI);
    }

    private PotvrdaRezervacije potvrda(RezervacijaTermina r) {
        DogadjajTermina d = r.getDogadjaj();
        Tvrtka t = d.getTvrtka();
        return new PotvrdaRezervacije(d.getNaziv(), t.getNaziv(), t.getTelefon(), d.getLokacija(), r.getDatum(),
                r.getVrijeme(), r.getNazivPaketa(), r.getTrajanjeMinuta(), r.getCijena(), r.getIme(),
                r.getOtkazano() != null, moguceOtkazati(r), terminProsao(r), OTKAZ_SATI);
    }

    private void posaljiPotvrdu(RezervacijaTermina r, String otkazniKod) {
        DogadjajTermina d = r.getDogadjaj();
        Tvrtka t = d.getTvrtka();
        StringBuilder tekst = new StringBuilder()
                .append("Pozdrav ").append(r.getIme()).append(",\n\n")
                .append("termin je rezerviran:\n\n")
                .append(d.getNaziv()).append('\n')
                .append(DAN.format(r.getDatum())).append(" u ").append(r.getVrijeme()).append('\n')
                .append(r.getNazivPaketa()).append(", ").append(r.getTrajanjeMinuta()).append(" min, ")
                .append(novac(r)).append('\n');
        if (d.getLokacija() != null) {
            tekst.append(d.getLokacija()).append('\n');
        }
        LocalDateTime rok = rokOtkaza(r);
        tekst.append("\nAko ne možeš doći, otkaži termin najkasnije ").append(OTKAZ_SATI)
                .append(" sata prije (do ").append(rok.format(TerminiService.DATUM)).append(" u ")
                .append(rok.toLocalTime()).append("), da ga može uzeti netko drugi:\n")
                .append(javnaAdresa).append("/rezervacija/otkazi/").append(otkazniKod).append('\n')
                .append("Kasnije od toga nazovi studio.\n\n")
                .append(t.getNaziv());
        if (t.getTelefon() != null) {
            tekst.append(", ").append(t.getTelefon());
        }
        posalji(r.getEmail(), "Termin je rezerviran: " + d.getNaziv(), tekst.toString());
    }

    private void obavijestiStudio(RezervacijaTermina r) {
        String email = r.getDogadjaj().getTvrtka().getEmail();
        if (email != null) {
            posalji(email, "Nova rezervacija: " + r.getDatum().format(TerminiService.DATUM) + " u " + r.getVrijeme(),
                    r.getIme() + ", " + r.getTelefon() + "\n" + r.getNazivPaketa() + ", " + novac(r)
                            + (r.getNapomena() == null ? "" : "\nNapomena: " + r.getNapomena())
                            + "\n\nSve rezervacije su u aplikaciji pod Termini.");
        }
    }

    private void obavijestiStudioOtkazano(RezervacijaTermina r) {
        String email = r.getDogadjaj().getTvrtka().getEmail();
        if (email != null) {
            posalji(email, "Otkazan termin: " + r.getDatum().format(TerminiService.DATUM) + " u " + r.getVrijeme(),
                    r.getIme() + " je otkazao/la termin. Vrijeme je ponovno slobodno.");
        }
    }

    // Rezervacija je već spremljena; ako mail ne ode, ne smije se poništiti
    private void posalji(String prima, String naslov, String tekst) {
        try {
            emailServis.posalji(prima, naslov, tekst);
        } catch (RuntimeException e) {
            log.warn("Mail za termin nije poslan: {}", e.getMessage());
        }
    }

    private static String novac(RezervacijaTermina r) {
        String uzorak = r.getCijena().stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00";
        return new DecimalFormat(uzorak, DecimalFormatSymbols.getInstance(HR)).format(r.getCijena()) + " €";
    }

    static String hash(String kod) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(kod.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
