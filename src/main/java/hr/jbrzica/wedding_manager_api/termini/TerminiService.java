package hr.jbrzica.wedding_manager_api.termini;

import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.security.TajniToken;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.BrojPoPaketu;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DanDto;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DanRaspored;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DogadjajDetalji;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DogadjajRequest;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DogadjajSazetak;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.Mjesto;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.PaketDto;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.RezervacijaResponse;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.RucnaRezervacijaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Termini za kratka fotografiranja, iz perspektive fotografa: događaji, raspored i ručne rezervacije.
 * Javnu rezervaciju preko linka radi {@link JavniTerminiService}.
 */
@Service
public class TerminiService {

    static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("d. M. yyyy.");

    private final DogadjajTerminaRepository dogadjajRepository;
    private final RezervacijaTerminaRepository rezervacijaRepository;
    private final TvrtkaRepository tvrtkaRepository;
    private final TrenutniKorisnik trenutniKorisnik;
    private final RezervacijaZapis zapis;

    public TerminiService(DogadjajTerminaRepository dogadjajRepository, RezervacijaTerminaRepository rezervacijaRepository,
                          TvrtkaRepository tvrtkaRepository, TrenutniKorisnik trenutniKorisnik, RezervacijaZapis zapis) {
        this.dogadjajRepository = dogadjajRepository;
        this.rezervacijaRepository = rezervacijaRepository;
        this.tvrtkaRepository = tvrtkaRepository;
        this.trenutniKorisnik = trenutniKorisnik;
        this.zapis = zapis;
    }

    @Transactional(readOnly = true)
    public List<DogadjajSazetak> popis() {
        List<DogadjajTermina> dogadjaji = dogadjajRepository.findByTvrtkaIdOrderByKreiranoDesc(trenutniKorisnik.tvrtkaId());
        Map<Long, List<RezervacijaTermina>> rezervacije = rezervacijaRepository
                .findByDogadjajIdInAndOtkazanoIsNull(dogadjaji.stream().map(DogadjajTermina::getId).toList())
                .stream().collect(Collectors.groupingBy(r -> r.getDogadjaj().getId()));
        return dogadjaji.stream().map(d -> {
            List<RezervacijaTermina> r = rezervacije.getOrDefault(d.getId(), List.of());
            return new DogadjajSazetak(d.getId(), d.getNaziv(),
                    d.getDani().stream().map(DanTermina::getDatum).min(Comparator.naturalOrder()).orElse(null),
                    d.getDani().stream().map(DanTermina::getDatum).max(Comparator.naturalOrder()).orElse(null),
                    d.isOtvoren(), ukupnoMjesta(d), r.size(), zbroj(r));
        }).toList();
    }

    @Transactional(readOnly = true)
    public DogadjajDetalji detalji(Long id) {
        return uDetalje(dohvati(id));
    }

    @Transactional
    public DogadjajDetalji stvori(DogadjajRequest request) {
        DogadjajTermina d = new DogadjajTermina();
        Tvrtka tvrtka = tvrtkaRepository.getReferenceById(trenutniKorisnik.tvrtkaId());
        d.setTvrtka(tvrtka);
        d.setJavniKod(TajniToken.novi());
        d.setKreirano(Instant.now());
        popuni(d, request, List.of());
        return uDetalje(dogadjajRepository.save(d));
    }

    @Transactional
    public DogadjajDetalji izmijeni(Long id, DogadjajRequest request) {
        DogadjajTermina d = dohvati(id);
        popuni(d, request, rezervacijaRepository.findByDogadjajIdAndOtkazanoIsNullOrderByDatumAscVrijemeAsc(id));
        return uDetalje(dogadjajRepository.save(d));
    }

    @Transactional
    public void obrisi(Long id) {
        dogadjajRepository.delete(dohvati(id));
    }

    // Stari link prestane raditi, npr. kad je završilo slikanje ili je link otišao krivim ljudima
    @Transactional
    public DogadjajDetalji noviLink(Long id) {
        DogadjajTermina d = dohvati(id);
        d.setJavniKod(TajniToken.novi());
        return uDetalje(d);
    }

    // Za nekoga tko je nazvao: bez maila, bez ograničenja, može i kad je link zatvoren
    @Transactional
    public RezervacijaResponse rucnaRezervacija(Long dogadjajId, RucnaRezervacijaRequest request) {
        DogadjajTermina d = dohvati(dogadjajId);
        PaketTermina paket = paket(d, request.paketId());
        provjeriMjesto(d, request.datum(), request.vrijeme());
        RezervacijaTermina r = zapis.spremi(d, paket, request.datum(), request.vrijeme(), request.ime(),
                request.telefon(), request.email(), request.napomena(), true, null);
        return uResponse(r);
    }

    @Transactional
    public void otkazi(Long rezervacijaId) {
        RezervacijaTermina r = rezervacijaRepository.findByIdAndDogadjajTvrtkaId(rezervacijaId, trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rezervacija ne postoji"));
        if (r.getOtkazano() == null) {
            r.setOtkazano(Instant.now());
        }
    }

    // ---------- pravila ----------

    private void popuni(DogadjajTermina d, DogadjajRequest request, List<RezervacijaTermina> aktivne) {
        int korak = request.korakMinuta();
        for (PaketDto p : request.paketi()) {
            // Svaki termin zauzme točno jedno mjesto u rasporedu; dulji paket bi se preklopio sa sljedećim
            if (p.trajanjeMinuta() > korak) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Paket „" + p.naziv() + "” traje " + p.trajanjeMinuta() + " min, a termini su svakih "
                                + korak + " min. Povećaj razmak između termina.");
            }
        }
        for (DanDto dan : request.dani()) {
            if (!dan.doVremena().isAfter(dan.od())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Na dan " + dan.datum().format(DATUM) + " kraj mora biti nakon početka.");
            }
        }

        d.setNaziv(request.naziv().trim());
        d.setOpis(prazno(request.opis()));
        d.setLokacija(prazno(request.lokacija()));
        d.setKorakMinuta(korak);
        d.setOtvoren(request.otvoren());

        // Paketi: postojeći se ažuriraju (rezervacije ostaju vezane), novi dodaju, uklonjeni brišu
        Map<Long, PaketTermina> postojeci = d.getPaketi().stream()
                .collect(Collectors.toMap(PaketTermina::getId, Function.identity()));
        List<PaketTermina> novi = new ArrayList<>();
        int redoslijed = 0;
        for (PaketDto p : request.paketi()) {
            PaketTermina paket = p.id() != null && postojeci.containsKey(p.id()) ? postojeci.get(p.id()) : new PaketTermina();
            paket.setDogadjaj(d);
            paket.setNaziv(p.naziv().trim());
            paket.setOpis(prazno(p.opis()));
            paket.setTrajanjeMinuta(p.trajanjeMinuta());
            paket.setCijena(p.cijena());
            paket.setRedoslijed(redoslijed++);
            novi.add(paket);
        }
        d.getPaketi().clear();
        d.getPaketi().addAll(novi);

        d.getDani().clear();
        for (DanDto dan : request.dani()) {
            DanTermina novDan = new DanTermina();
            novDan.setDogadjaj(d);
            novDan.setDatum(dan.datum());
            novDan.setOd(dan.od());
            novDan.setDoVremena(dan.doVremena());
            d.getDani().add(novDan);
        }

        // Promjena dana, sati ili razmaka ne smije ostaviti rezervaciju izvan rasporeda
        for (RezervacijaTermina r : aktivne) {
            if (!mjesta(d, r.getDatum()).contains(r.getVrijeme())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Na " + r.getDatum().format(DATUM) + " u " + r.getVrijeme() + " je rezervacija ("
                                + r.getIme() + "). Taj termin mora ostati u rasporedu ili ga prvo otkaži.");
            }
        }
    }

    void provjeriMjesto(DogadjajTermina d, LocalDate datum, LocalTime vrijeme) {
        if (!mjesta(d, datum).contains(vrijeme)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taj termin ne postoji u rasporedu.");
        }
    }

    static PaketTermina paket(DogadjajTermina d, Long paketId) {
        return d.getPaketi().stream().filter(p -> p.getId().equals(paketId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paket ne postoji."));
    }

    // Sva vremena početka na jedan datum: od početka dana svakih N minuta, dok cijeli razmak stane do kraja
    static List<LocalTime> mjesta(DogadjajTermina d, LocalDate datum) {
        List<LocalTime> vremena = new ArrayList<>();
        for (DanTermina dan : d.getDani()) {
            if (!dan.getDatum().equals(datum)) {
                continue;
            }
            int od = dan.getOd().toSecondOfDay() / 60;
            int kraj = dan.getDoVremena().toSecondOfDay() / 60;
            for (int m = od; m + d.getKorakMinuta() <= kraj; m += d.getKorakMinuta()) {
                vremena.add(LocalTime.of(m / 60, m % 60));
            }
        }
        return vremena.stream().distinct().sorted().toList();
    }

    static int ukupnoMjesta(DogadjajTermina d) {
        return d.getDani().stream().map(DanTermina::getDatum).distinct().mapToInt(datum -> mjesta(d, datum).size()).sum();
    }

    private DogadjajDetalji uDetalje(DogadjajTermina d) {
        List<RezervacijaTermina> aktivne = d.getId() == null ? List.of()
                : rezervacijaRepository.findByDogadjajIdAndOtkazanoIsNullOrderByDatumAscVrijemeAsc(d.getId());
        Map<String, RezervacijaTermina> poMjestu = new HashMap<>();
        for (RezervacijaTermina r : aktivne) {
            poMjestu.put(r.getDatum() + " " + r.getVrijeme(), r);
        }
        List<DanRaspored> raspored = new ArrayList<>();
        for (DanTermina dan : d.getDani()) {
            List<Mjesto> mjesta = mjesta(d, dan.getDatum()).stream()
                    .filter(v -> !v.isBefore(dan.getOd()) && v.isBefore(dan.getDoVremena()))
                    .map(v -> {
                        RezervacijaTermina r = poMjestu.get(dan.getDatum() + " " + v);
                        return new Mjesto(v, r == null ? null : uResponse(r));
                    }).toList();
            raspored.add(new DanRaspored(dan.getDatum(), dan.getOd(), dan.getDoVremena(), mjesta));
        }
        Map<String, Long> poPaketu = new LinkedHashMap<>();
        d.getPaketi().forEach(p -> poPaketu.put(p.getNaziv(), 0L));
        aktivne.forEach(r -> poPaketu.merge(r.getNazivPaketa(), 1L, Long::sum));
        List<PaketDto> paketi = d.getPaketi().stream()
                .map(p -> new PaketDto(p.getId(), p.getNaziv(), p.getOpis(), p.getTrajanjeMinuta(), p.getCijena()))
                .toList();
        return new DogadjajDetalji(d.getId(), d.getNaziv(), d.getOpis(), d.getLokacija(), d.getKorakMinuta(),
                d.isOtvoren(), d.getJavniKod(), paketi, raspored, ukupnoMjesta(d), aktivne.size(), zbroj(aktivne),
                poPaketu.entrySet().stream().map(e -> new BrojPoPaketu(e.getKey(), e.getValue())).toList());
    }

    static RezervacijaResponse uResponse(RezervacijaTermina r) {
        return new RezervacijaResponse(r.getId(), r.getDatum(), r.getVrijeme(),
                r.getPaket() == null ? null : r.getPaket().getId(), r.getNazivPaketa(), r.getTrajanjeMinuta(),
                r.getCijena(), r.getIme(), r.getTelefon(), r.getEmail(), r.getNapomena(), r.isRucno(), r.getKreirano());
    }

    private static BigDecimal zbroj(List<RezervacijaTermina> rezervacije) {
        return rezervacije.stream().map(RezervacijaTermina::getCijena).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DogadjajTermina dohvati(Long id) {
        return dogadjajRepository.findByIdAndTvrtkaId(id, trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Događaj ne postoji"));
    }

    static String prazno(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
