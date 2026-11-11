package hr.jbrzica.wedding_manager_api.ugovor;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.entity.Osoba;
import hr.jbrzica.wedding_manager_api.entity.StavkaRasporeda;
import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import hr.jbrzica.wedding_manager_api.service.TvrtkaService;
import hr.jbrzica.wedding_manager_api.service.VjencanjeService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ugovor iz podataka vjenčanja. Svaka tvrtka može učitati svoj Word predložak s {{oznakama}};
 * dok to ne napravi, koristi se zadani predložak napravljen po papirnatom ugovoru.
 */
@Service
public class UgovorService {

    static final String ZADANI = "ugovor/zadani-ugovor.docx";
    static final int NAJVECI_PREDLOZAK = 2 * 1024 * 1024;
    // Prazno polje postaje crta, da se može dopisati rukom kao na papirnatom ugovoru
    static final String CRTA = "______________________";

    // Ključne riječi po kojima se u rasporedu prepozna obred (crkva ili matičar)
    private static final List<String> OBRED = List.of("crkv", "vjenčanj", "vjencanj", "obred", "matič", "matic");
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("d. M. yyyy.");
    private static final Locale HR = Locale.forLanguageTag("hr-HR");
    // Datum ugovora po hrvatskom vremenu, i kad server radi u UTC-u
    static final ZoneId ZONA = ZoneId.of("Europe/Zagreb");

    private final VjencanjeService vjencanjeService;
    private final TvrtkaService tvrtkaService;
    private final PredlozakUgovoraRepository predlozakRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public UgovorService(VjencanjeService vjencanjeService, TvrtkaService tvrtkaService,
                         PredlozakUgovoraRepository predlozakRepository, TrenutniKorisnik trenutniKorisnik) {
        this.vjencanjeService = vjencanjeService;
        this.tvrtkaService = tvrtkaService;
        this.predlozakRepository = predlozakRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @Transactional(readOnly = true)
    public Datoteka ugovor(Long vjencanjeId) {
        Vjencanje v = vjencanjeService.dohvatiEntitet(vjencanjeId);
        byte[] predlozak = predlozak().sadrzaj();
        try {
            byte[] ugovor = WordPredlozak.popuni(predlozak, vrijednosti(v));
            String naziv = "Ugovor - " + narucitelj(v) + " - " + v.getDatum().format(DateTimeFormatter.ofPattern("d.M.yyyy")) + ".docx";
            return new Datoteka(naziv, ugovor);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Predložak ugovora nije moguće otvoriti. Učitajte ga ponovno u Postavkama.");
        }
    }

    @Transactional(readOnly = true)
    public PredlozakInfo info() {
        return predlozakRepository.findById(trenutniKorisnik.tvrtkaId())
                .map(p -> new PredlozakInfo(true, p.getNazivDatoteke(), p.getAzurirano()))
                .orElse(new PredlozakInfo(false, "zadani-ugovor.docx", null));
    }

    @Transactional(readOnly = true)
    public Datoteka predlozak() {
        return predlozakRepository.findById(trenutniKorisnik.tvrtkaId())
                .map(p -> new Datoteka(p.getNazivDatoteke(), p.getSadrzaj()))
                .orElseGet(UgovorService::zadani);
    }

    @Transactional
    public PredlozakInfo spremi(String nazivDatoteke, byte[] sadrzaj) {
        tvrtkaService.provjeriVlasnika();
        if (sadrzaj == null || sadrzaj.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datoteka je prazna");
        }
        if (sadrzaj.length > NAJVECI_PREDLOZAK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Predložak smije imati najviše 2 MB");
        }
        if (nazivDatoteke == null || !nazivDatoteke.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Predložak mora biti Word datoteka (.docx). Stari .doc spremite kao .docx.");
        }
        // Otvori ga odmah, da se loša datoteka odbije sad, a ne tek kad netko zatraži ugovor
        try (XWPFDocument ignored = new XWPFDocument(new ByteArrayInputStream(sadrzaj))) {
            WordPredlozak.popuni(sadrzaj, Map.of());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datoteka nije ispravan Word (.docx) dokument");
        }

        PredlozakUgovora p = predlozakRepository.findById(trenutniKorisnik.tvrtkaId()).orElseGet(PredlozakUgovora::new);
        p.setTvrtkaId(trenutniKorisnik.tvrtkaId());
        p.setNazivDatoteke(nazivDatoteke);
        p.setSadrzaj(sadrzaj);
        p.setAzurirano(Instant.now());
        predlozakRepository.save(p);
        return new PredlozakInfo(true, nazivDatoteke, p.getAzurirano());
    }

    @Transactional
    public PredlozakInfo vratiZadani() {
        tvrtkaService.provjeriVlasnika();
        predlozakRepository.deleteById(trenutniKorisnik.tvrtkaId());
        return new PredlozakInfo(false, "zadani-ugovor.docx", null);
    }

    static Datoteka zadani() {
        try (InputStream in = new ClassPathResource(ZADANI).getInputStream()) {
            return new Datoteka("zadani-ugovor.docx", in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- vrijednosti oznaka ----------

    Map<String, String> vrijednosti(Vjencanje v) {
        Tvrtka t = v.getTvrtka();
        Map<String, String> m = new HashMap<>();
        m.put("tvrtka", ili(t.getNaziv()));
        m.put("tvrtka_adresa", ili(t.getAdresa()));
        m.put("tvrtka_oib", ili(t.getOib()));
        m.put("tvrtka_telefon", ili(t.getTelefon()));
        m.put("tvrtka_email", ili(t.getEmail()));

        m.put("narucitelj", narucitelj(v));
        osoba(m, "mlada", v.getMlada());
        osoba(m, "mladozenja", v.getMladozenja());

        m.put("datum", v.getDatum().format(DATUM));
        m.put("vjencanje", ili(obred(v)));
        m.put("dvorana", ili(v.getLokacija()));
        m.put("broj_gostiju", v.getBrojGostiju() == null ? CRTA : v.getBrojGostiju().toString());
        m.put("napomene", ili(v.getNapomene() == null ? null : v.getNapomene().replaceAll("\\s+", " ")));

        m.put("paket", ili(v.getPaket() == null ? null : v.getPaket().getNaziv()));
        m.put("cijena", novac(v.getDogovorenaCijena()));
        BigDecimal kapara = zbroj(v.getUplate().stream().filter(u -> u.getVrsta() == VrstaUplate.KAPARA).toList());
        m.put("akontacija", kapara.signum() > 0 ? novac(kapara) : CRTA);
        m.put("uplaceno", novac(zbroj(v.getUplate())));
        m.put("ostatak", novac(v.getDogovorenaCijena().subtract(zbroj(v.getUplate())).max(BigDecimal.ZERO)));
        m.put("dodaci", ili(v.getDodaci().stream()
                .map(Dodatak::getNaziv)
                .sorted(Comparator.comparing(String::toLowerCase))
                .collect(Collectors.joining(", "))));

        m.put("danas", LocalDate.now(ZONA).format(DATUM));
        return m;
    }

    private static void osoba(Map<String, String> m, String kljuc, Osoba o) {
        m.put(kljuc, o == null ? CRTA : ili(o.punoIme()));
        m.put(kljuc + "_adresa", ili(o == null ? null : o.getAdresa()));
        m.put(kljuc + "_telefon", ili(o == null ? null : o.getTelefon()));
        m.put(kljuc + "_email", ili(o == null ? null : o.getEmail()));
        m.put(kljuc + "_oib", ili(o == null ? null : o.getOib()));
    }

    static String narucitelj(Vjencanje v) {
        String mlada = v.getMlada() == null ? "" : v.getMlada().punoIme().trim();
        String mladozenja = v.getMladozenja() == null ? "" : v.getMladozenja().punoIme().trim();
        if (mlada.isEmpty() || mladozenja.isEmpty()) {
            return mlada + mladozenja;
        }
        return mlada + " i " + mladozenja;
    }

    // Crkva ili matičar iz rasporeda dana: adresa ako je upisana, inače naziv stavke
    static String obred(Vjencanje v) {
        for (StavkaRasporeda s : v.getRaspored()) {
            String naziv = s.getNaziv() == null ? "" : s.getNaziv().toLowerCase(HR);
            if (OBRED.stream().anyMatch(naziv::contains)) {
                return s.getAdresa() != null && !s.getAdresa().isBlank() ? s.getAdresa() : s.getNaziv();
            }
        }
        return null;
    }

    private static BigDecimal zbroj(List<Uplata> uplate) {
        return uplate.stream().map(Uplata::getIznos).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static String novac(BigDecimal iznos) {
        DecimalFormatSymbols simboli = DecimalFormatSymbols.getInstance(HR);
        String uzorak = iznos.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00";
        return new DecimalFormat(uzorak, simboli).format(iznos) + " €";
    }

    private static String ili(String s) {
        return s == null || s.isBlank() ? CRTA : s.trim();
    }
}
