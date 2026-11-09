package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.PromjenaResponse;
import hr.jbrzica.wedding_manager_api.entity.Osoba;
import hr.jbrzica.wedding_manager_api.entity.Promjena;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import hr.jbrzica.wedding_manager_api.repository.PromjenaRepository;
import hr.jbrzica.wedding_manager_api.security.KorisnikPrincipal;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Povijest bitnih promjena. Zapis ide u istu transakciju kao i sama promjena,
 * pa se zapisuje samo ono što je stvarno spremljeno.
 */
@Service
public class PovijestServis {

    private static final Map<StatusVjencanja, String> STATUSI = Map.of(
            StatusVjencanja.UPIT, "Upit", StatusVjencanja.POTVRDENO, "Potvrđeno",
            StatusVjencanja.ODRADENO, "Odrađeno", StatusVjencanja.ISPORUCENO, "Isporučeno",
            StatusVjencanja.OTKAZANO, "Otkazano");
    private static final Map<VrstaUplate, String> VRSTE = Map.of(
            VrstaUplate.KAPARA, "kapara", VrstaUplate.RATA, "rata", VrstaUplate.OSTATAK, "ostatak");
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("d. M. yyyy.");
    private static final int NAJVISE = 200;

    private final PromjenaRepository repository;
    private final KorisnikRepository korisnikRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public PovijestServis(PromjenaRepository repository, KorisnikRepository korisnikRepository,
                          TrenutniKorisnik trenutniKorisnik) {
        this.repository = repository;
        this.korisnikRepository = korisnikRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @Transactional
    public void zabiljezi(Vjencanje v, String opis) {
        zabiljezi(v.getId(), naziv(v), opis);
    }

    @Transactional
    public void zabiljezi(Long vjencanjeId, String vjencanjeNaziv, String opis) {
        KorisnikPrincipal ja = trenutniKorisnik.get();
        Promjena p = new Promjena();
        p.setTvrtkaId(ja.tvrtkaId());
        p.setVjencanjeId(vjencanjeId);
        p.setVjencanjeNaziv(vjencanjeNaziv);
        p.setKorisnikId(ja.id());
        p.setKorisnikIme(korisnikRepository.findById(ja.id()).map(k -> k.getIme()).orElse(ja.email()));
        p.setVrijeme(Instant.now());
        p.setOpis(opis.length() > 500 ? opis.substring(0, 500) : opis);
        repository.save(p);
    }

    @Transactional(readOnly = true)
    public List<PromjenaResponse> zaVjencanje(Long vjencanjeId) {
        return repository.findByTvrtkaIdAndVjencanjeIdOrderByVrijemeDescIdDesc(trenutniKorisnik.tvrtkaId(), vjencanjeId)
                .stream().map(PromjenaResponse::iz).toList();
    }

    @Transactional(readOnly = true)
    public List<PromjenaResponse> zaTvrtku(Long korisnikId) {
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();
        PageRequest prvih = PageRequest.of(0, NAJVISE);
        List<Promjena> promjene = korisnikId == null
                ? repository.findByTvrtkaIdOrderByVrijemeDescIdDesc(tvrtkaId, prvih)
                : repository.findByTvrtkaIdAndKorisnikIdOrderByVrijemeDescIdDesc(tvrtkaId, korisnikId, prvih);
        return promjene.stream().map(PromjenaResponse::iz).toList();
    }

    // ---------- tekstovi ----------

    public static String naziv(Vjencanje v) {
        return (ime(v.getMlada()) + " & " + ime(v.getMladozenja())).trim();
    }

    public static String status(StatusVjencanja s) {
        return STATUSI.get(s);
    }

    public static String vrsta(VrstaUplate v) {
        return VRSTE.get(v);
    }

    public static String novac(BigDecimal iznos) {
        if (iznos == null) {
            return "–";
        }
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("hr-HR"));
        f.setMaximumFractionDigits(2);
        return f.format(iznos) + " €";
    }

    public static String datum(LocalDate d) {
        return d == null ? "–" : DATUM.format(d);
    }

    private static String ime(Osoba o) {
        return o == null || o.getIme() == null ? "" : o.getIme();
    }
}
