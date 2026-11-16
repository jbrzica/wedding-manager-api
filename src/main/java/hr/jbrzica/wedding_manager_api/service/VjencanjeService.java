package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.entity.*;
import hr.jbrzica.wedding_manager_api.repository.ClanEkipeRepository;
import hr.jbrzica.wedding_manager_api.repository.PaketVjencanja;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeFilter;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
import hr.jbrzica.wedding_manager_api.repository.ZbrojPoVjencanju;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class VjencanjeService {

    // Svako novo vjenčanje dobije ovu kontrolnu listu; stavke se mogu mijenjati
    private static final List<String> ZADANI_ZADACI = List.of(
            "Ugovor poslan", "Ugovor potpisan", "Odabir fotografija", "Album naručen", "Galerija poslana");

    // Upiti i otkazana vjenčanja nisu dogovoreni posao
    private static final List<StatusVjencanja> NIJE_POSAO = List.of(StatusVjencanja.UPIT, StatusVjencanja.OTKAZANO);

    private final VjencanjeRepository vjencanjeRepository;
    private final UplataRepository uplataRepository;
    private final ClanEkipeRepository clanEkipeRepository;
    private final PaketService paketService;
    private final DodatakService dodatakService;
    private final SuradnikService suradnikService;
    private final FinancijeKalkulator financijeKalkulator;
    private final TvrtkaRepository tvrtkaRepository;
    private final TrenutniKorisnik trenutniKorisnik;
    private final PovijestServis povijest;
    private final PrognozaServis prognozaServis;

    public VjencanjeService(VjencanjeRepository vjencanjeRepository,
                            UplataRepository uplataRepository,
                            ClanEkipeRepository clanEkipeRepository,
                            PaketService paketService,
                            DodatakService dodatakService,
                            SuradnikService suradnikService,
                            FinancijeKalkulator financijeKalkulator,
                            TvrtkaRepository tvrtkaRepository,
                            TrenutniKorisnik trenutniKorisnik,
                            PovijestServis povijest,
                            PrognozaServis prognozaServis) {
        this.povijest = povijest;
        this.prognozaServis = prognozaServis;
        this.vjencanjeRepository = vjencanjeRepository;
        this.uplataRepository = uplataRepository;
        this.clanEkipeRepository = clanEkipeRepository;
        this.paketService = paketService;
        this.dodatakService = dodatakService;
        this.suradnikService = suradnikService;
        this.financijeKalkulator = financijeKalkulator;
        this.tvrtkaRepository = tvrtkaRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    // ---------- čitanje ----------

    // Najviše vjenčanja na jednoj stranici, da netko ne može zatražiti sve odjednom
    public static final int NAJVISE_PO_STRANICI = 100;

    public Page<VjencanjeSazetak> popis(Razdoblje razdoblje, Integer godina, List<StatusVjencanja> statusi,
                                        String pretraga, boolean nenaplaceno, boolean kasniIsporuka,
                                        boolean bezKapare, int stranica, int velicina) {
        VjencanjeFilter filter = new VjencanjeFilter(trenutniKorisnik.tvrtkaId(), razdoblje, godina, statusi,
                pretraga, nenaplaceno, kasniIsporuka, bezKapare, LocalDate.now());
        // Prošla vjenčanja od najnovijeg, ostala od najbližeg; id da redoslijed bude stalan između stranica
        Sort redoslijed = razdoblje == Razdoblje.PROSLA
                ? Sort.by(Sort.Order.desc("datum"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.asc("datum"), Sort.Order.asc("id"));
        Pageable zahtjev = PageRequest.of(Math.max(stranica, 0), Math.clamp(velicina, 1, NAJVISE_PO_STRANICI), redoslijed);

        Page<Vjencanje> vjencanja = vjencanjeRepository.findAll(filter.uSpecifikaciju(), zahtjev);
        return new PageImpl<>(uSazetke(vjencanja.getContent()), zahtjev, vjencanja.getTotalElements());
    }

    public List<VjencanjeSazetak> kalendar(LocalDate od, LocalDate doDatuma) {
        if (od.isAfter(doDatuma)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Početni datum ne smije biti nakon završnog");
        }
        return uSazetke(vjencanjeRepository.findByTvrtkaIdAndDatumBetweenOrderByDatumAsc(trenutniKorisnik.tvrtkaId(), od, doDatuma));
    }

    public VjencanjeDetalji detalji(Long id) {
        return uDetalje(dohvatiEntitet(id));
    }

    public FinancijeResponse financije(Long id) {
        return financijeKalkulator.izracunaj(dohvatiEntitet(id));
    }

    public PregledResponse pregled() {
        LocalDate danas = LocalDate.now();
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();

        Optional<Vjencanje> sljedece = vjencanjeRepository
                .findFirstByTvrtkaIdAndDatumGreaterThanEqualAndStatusNotInOrderByDatumAsc(
                        tvrtkaId, danas, NIJE_POSAO);
        VjencanjeSazetak sljedeceSazetak = sljedece.map(v -> uSazetke(List.of(v)).get(0)).orElse(null);
        Long danaDoSljedeceg = sljedece.map(v -> ChronoUnit.DAYS.between(danas, v.getDatum())).orElse(null);

        long brojOveGodine = vjencanjeRepository.countByTvrtkaIdAndDatumBetweenAndStatusNotIn(
                tvrtkaId,
                LocalDate.of(danas.getYear(), 1, 1),
                LocalDate.of(danas.getYear(), 12, 31),
                NIJE_POSAO);

        BigDecimal ukupnoNenaplaceno = BigDecimal.ZERO;
        for (VjencanjeSazetak s : uSazetke(vjencanjeRepository.findByTvrtkaIdAndStatusNotIn(tvrtkaId, NIJE_POSAO))) {
            if (s.preostalo().signum() > 0) {
                ukupnoNenaplaceno = ukupnoNenaplaceno.add(s.preostalo());
            }
        }

        // Vrijeme prve stavke rasporeda: kad ekipa taj dan kreće
        LocalTime pocetak = sljedece.flatMap(v -> v.getRaspored().stream()
                .map(StavkaRasporeda::getVrijeme)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())).orElse(null);
        int ekipa = sljedece.map(v -> v.getEkipa().size()).orElse(0);

        return new PregledResponse(sljedeceSazetak, danaDoSljedeceg, pocetak, ekipa, brojOveGodine, ukupnoNenaplaceno,
                prebroji(tvrtkaId, Razdoblje.SVA, null, true, false, danas),
                prebroji(tvrtkaId, Razdoblje.NADOLAZECA, null, false, true, danas),
                prebroji(tvrtkaId, Razdoblje.NADOLAZECA, List.of(StatusVjencanja.UPIT), false, false, danas));
    }

    // Isti filter kao u popisu, da broj na početnom zaslonu odgovara onome što se otvori dodirom
    private long prebroji(Long tvrtkaId, Razdoblje razdoblje, List<StatusVjencanja> statusi,
                          boolean kasniIsporuka, boolean bezKapare, LocalDate danas) {
        return vjencanjeRepository.count(new VjencanjeFilter(tvrtkaId, razdoblje, null, statusi, null,
                false, kasniIsporuka, bezKapare, danas).uSpecifikaciju());
    }

    // Prognoza za vjenčanja idućih 7 dana, po id-u vjenčanja
    public Map<Long, PrognozaResponse> prognoze() {
        LocalDate danas = LocalDate.now();
        Map<Long, PrognozaResponse> rezultat = new HashMap<>();
        for (Vjencanje v : vjencanjeRepository.findByTvrtkaIdAndDatumBetweenOrderByDatumAsc(
                trenutniKorisnik.tvrtkaId(), danas, danas.plusDays(PrognozaServis.DANA_UNAPRIJED))) {
            if (v.getStatus() != StatusVjencanja.OTKAZANO) {
                prognozaServis.za(v.getLokacija(), v.getDatum()).ifPresent(p -> rezultat.put(v.getId(), p));
            }
        }
        return rezultat;
    }

    public List<PromjenaResponse> povijest(Long id) {
        dohvatiEntitet(id);   // provjerava da je vjenčanje od moje tvrtke
        return povijest.zaVjencanje(id);
    }

    // Brza provjera datuma: tko je taj dan zauzet i ima li vjenčanja koji dan prije ili poslije
    public DostupnostResponse dostupnost(LocalDate datum) {
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();
        List<Vjencanje> tjedan = vjencanjeRepository.findByTvrtkaIdAndDatumBetweenOrderByDatumAsc(
                        tvrtkaId, datum.minusDays(3), datum.plusDays(3)).stream()
                .filter(v -> v.getStatus() != StatusVjencanja.OTKAZANO)
                .toList();
        List<Vjencanje> tajDan = tjedan.stream().filter(v -> v.getDatum().equals(datum)).toList();
        List<Vjencanje> blizu = tjedan.stream().filter(v -> !v.getDatum().equals(datum)).toList();

        Map<Long, String> zauzeti = new HashMap<>();
        for (ClanEkipe c : clanEkipeRepository.findByVjencanjeDatumAndVjencanjeTvrtkaIdAndVjencanjeObrisanoIsNull(datum, tvrtkaId)) {
            if (c.getVjencanje().getStatus() != StatusVjencanja.OTKAZANO) {
                zauzeti.putIfAbsent(c.getSuradnik().getId(), parMladenaca(c.getVjencanje()));
            }
        }
        List<DostupnostResponse.SuradnikDostupnost> suradnici = suradnikService.sviEntiteti().stream()
                .map(s -> new DostupnostResponse.SuradnikDostupnost(s.getId(), s.getIme(), s.getUloga(), zauzeti.get(s.getId())))
                .toList();

        return new DostupnostResponse(datum, tajDan.isEmpty(), uSazetke(tajDan), uSazetke(blizu), suradnici);
    }

    public UpozorenjaResponse provjeriPreklapanja(LocalDate datum, List<Long> suradnikIds, Long izuzmiId) {
        List<String> upozorenja = new ArrayList<>();
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();

        for (Vjencanje v : vjencanjeRepository.findByTvrtkaIdAndDatum(tvrtkaId, datum)) {
            if (!v.getId().equals(izuzmiId) && v.getStatus() != StatusVjencanja.OTKAZANO) {
                upozorenja.add("Na taj datum već imaš vjenčanje: " + parMladenaca(v));
            }
        }

        if (suradnikIds != null && !suradnikIds.isEmpty()) {
            for (ClanEkipe clan : clanEkipeRepository.findBySuradnikIdInAndVjencanjeDatumAndVjencanjeTvrtkaIdAndVjencanjeObrisanoIsNull(
                    suradnikIds, datum, tvrtkaId)) {
                Vjencanje drugo = clan.getVjencanje();
                if (!drugo.getId().equals(izuzmiId) && drugo.getStatus() != StatusVjencanja.OTKAZANO) {
                    upozorenja.add(clan.getSuradnik().getIme() + " je taj dan već na vjenčanju: "
                            + parMladenaca(clan.getVjencanje()));
                }
            }
        }

        return new UpozorenjaResponse(upozorenja);
    }

    public Vjencanje dohvatiEntitet(Long id) {
        // Tuđe vjenčanje se ponaša kao da ne postoji (404), da se ne otkrije da postoji
        return vjencanjeRepository.findByIdAndTvrtkaId(id, trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vjenčanje s id " + id + " ne postoji"));
    }

    // ---------- pisanje ----------

    @Transactional
    public VjencanjeDetalji stvori(VjencanjeRequest request) {
        Vjencanje vjencanje = new Vjencanje();
        vjencanje.setTvrtka(tvrtkaRepository.getReferenceById(trenutniKorisnik.tvrtkaId()));
        popuni(vjencanje, request);
        for (int i = 0; i < ZADANI_ZADACI.size(); i++) {
            Zadatak zadatak = new Zadatak();
            zadatak.setNaziv(ZADANI_ZADACI.get(i));
            zadatak.setRedoslijed(i);
            zadatak.setVjencanje(vjencanje);
            vjencanje.getZadaci().add(zadatak);
        }
        Vjencanje spremljeno = vjencanjeRepository.save(vjencanje);
        povijest.zabiljezi(spremljeno, "Upisano vjenčanje");
        return uDetalje(spremljeno);
    }

    // Brzi upit: ime onoga tko se javio ide pod mladu, mladoženja se dopuni kasnije
    @Transactional
    public VjencanjeDetalji stvoriUpit(BrziUpitRequest upit) {
        String[] dijelovi = upit.ime().trim().split("\\s+", 2);
        OsobaDto mlada = new OsobaDto(dijelovi[0], dijelovi.length > 1 ? dijelovi[1] : "",
                prazanUNull(upit.telefon()), null, null, null);
        // Prazan tekst, a ne null: da su sva polja null, Hibernate bi cijelog mladoženju vratio kao null
        OsobaDto mladozenja = new OsobaDto("", "", null, null, null, null);
        return stvori(new VjencanjeRequest(upit.datum(), null, mlada, mladozenja, null, List.of(), null, null, null,
                prazanUNull(upit.napomena()), null, null, null, List.of(), List.of()));
    }

    // Link se prikazuje kao poveznica, pa smije biti samo http(s): javascript: ili data: bi se izvršio na klik.
    // "galerija.hr/ana" bez sheme se dopuni s https://, da ne pukne uređivanje starih vjenčanja.
    static String linkGalerije(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        String l = link.trim();
        if (!l.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) {
            l = "https://" + l;
        }
        if (!l.matches("(?i)^https?://\\S+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link galerije mora počinjati s http:// ili https://");
        }
        return l;
    }

    private static String prazanUNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @Transactional
    public VjencanjeDetalji izmijeni(Long id, VjencanjeRequest request) {
        Vjencanje vjencanje = dohvatiEntitet(id);
        Snimka prije = new Snimka(vjencanje);
        popuni(vjencanje, request);
        zabiljeziRazlike(vjencanje, prije, new Snimka(vjencanje));
        return uDetalje(vjencanje);
    }

    @Transactional
    public VjencanjeDetalji promijeniStatus(Long id, StatusVjencanja status) {
        Vjencanje vjencanje = dohvatiEntitet(id);
        if (vjencanje.getStatus() != status) {
            povijest.zabiljezi(vjencanje, "Status: " + PovijestServis.status(vjencanje.getStatus())
                    + " → " + PovijestServis.status(status));
        }
        vjencanje.setStatus(status);
        return uDetalje(vjencanje);
    }

    // Ne briše se odmah: ide u koš, odakle se može vratiti 30 dana
    @Transactional
    public void obrisi(Long id) {
        Vjencanje vjencanje = dohvatiEntitet(id);
        vjencanje.setObrisano(Instant.now());
        povijest.zabiljezi(vjencanje, "Premješteno u koš");
    }

    // Stanje bitnih polja prije i poslije izmjene, za povijest promjena
    private record Snimka(LocalDate datum, String lokacija, BigDecimal cijena, String paket,
                          BigDecimal ostaliTroskovi, Map<Long, BigDecimal> ekipa) {
        Snimka(Vjencanje v) {
            this(v.getDatum(), v.getLokacija(), v.getDogovorenaCijena(),
                    v.getPaket() != null ? v.getPaket().getNaziv() : null, v.getOstaliTroskovi(), ekipaOd(v));
        }

        private static Map<Long, BigDecimal> ekipaOd(Vjencanje v) {
            Map<Long, BigDecimal> m = new HashMap<>();
            for (ClanEkipe c : v.getEkipa()) {
                m.put(c.getSuradnik().getId(), c.getHonorar() == null ? null : c.getHonorar().stripTrailingZeros());
            }
            return m;
        }
    }

    private void zabiljeziRazlike(Vjencanje v, Snimka prije, Snimka poslije) {
        if (!Objects.equals(prije.datum(), poslije.datum())) {
            povijest.zabiljezi(v, "Datum: " + PovijestServis.datum(prije.datum()) + " → " + PovijestServis.datum(poslije.datum()));
        }
        if (prije.cijena() == null ? poslije.cijena() != null : prije.cijena().compareTo(poslije.cijena()) != 0) {
            povijest.zabiljezi(v, "Cijena: " + PovijestServis.novac(prije.cijena()) + " → " + PovijestServis.novac(poslije.cijena()));
        }
        if (!Objects.equals(prije.paket(), poslije.paket())) {
            povijest.zabiljezi(v, "Paket: " + (prije.paket() == null ? "bez paketa" : prije.paket())
                    + " → " + (poslije.paket() == null ? "bez paketa" : poslije.paket()));
        }
        if (!Objects.equals(prazno(prije.lokacija()), prazno(poslije.lokacija()))) {
            povijest.zabiljezi(v, "Lokacija: " + (prazno(prije.lokacija()) == null ? "–" : prije.lokacija())
                    + " → " + (prazno(poslije.lokacija()) == null ? "–" : poslije.lokacija()));
        }
        BigDecimal stariTroskovi = prije.ostaliTroskovi() == null ? BigDecimal.ZERO : prije.ostaliTroskovi();
        BigDecimal noviTroskovi = poslije.ostaliTroskovi() == null ? BigDecimal.ZERO : poslije.ostaliTroskovi();
        if (stariTroskovi.compareTo(noviTroskovi) != 0) {
            povijest.zabiljezi(v, "Ostali troškovi: " + PovijestServis.novac(stariTroskovi) + " → " + PovijestServis.novac(noviTroskovi));
        }
        if (!prije.ekipa().equals(poslije.ekipa())) {
            povijest.zabiljezi(v, "Promijenjena ekipa ili honorari");
        }
    }

    private static String prazno(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ---------- pomoćne metode ----------

    private void popuni(Vjencanje vjencanje, VjencanjeRequest request) {
        vjencanje.setDatum(request.datum());
        vjencanje.setLokacija(request.lokacija());
        vjencanje.setMlada(request.mlada().uEntitet());
        vjencanje.setMladozenja(request.mladozenja().uEntitet());
        vjencanje.setRokIsporuke(request.rokIsporuke());
        vjencanje.setIzvorUpita(request.izvorUpita());
        vjencanje.setNapomene(request.napomene());
        vjencanje.setBrojGostiju(request.brojGostiju());
        vjencanje.setOstaliTroskovi(request.ostaliTroskovi());
        vjencanje.setLinkGalerije(linkGalerije(request.linkGalerije()));

        Paket paket = request.paketId() != null ? paketService.dohvatiEntitet(request.paketId()) : null;
        vjencanje.setPaket(paket);

        List<Dodatak> dodaci = dodatakService.dohvatiEntitete(request.dodatakIds());
        vjencanje.getDodaci().clear();
        vjencanje.getDodaci().addAll(dodaci);

        // Cijena se predloži iz paketa i dodataka, ali korisnik je može promijeniti.
        vjencanje.setDogovorenaCijena(request.dogovorenaCijena() != null
                ? request.dogovorenaCijena()
                : financijeKalkulator.predlozenaCijena(paket, dodaci));

        // Liste se ne zamjenjuju novima, nego se isprazne i napune:
        // tako Hibernate (orphanRemoval) obriše stare stavke iz baze.
        vjencanje.getRaspored().clear();
        if (request.raspored() != null) {
            for (StavkaRasporedaDto dto : request.raspored()) {
                StavkaRasporeda stavka = new StavkaRasporeda();
                stavka.setVrijeme(dto.vrijeme());
                stavka.setNaziv(dto.naziv());
                stavka.setAdresa(dto.adresa());
                stavka.setNapomena(dto.napomena());
                stavka.setVjencanje(vjencanje);
                vjencanje.getRaspored().add(stavka);
            }
        }

        vjencanje.getEkipa().clear();
        if (request.ekipa() != null) {
            for (ClanEkipeRequest dto : request.ekipa()) {
                Suradnik suradnik = suradnikService.dohvatiEntitet(dto.suradnikId());
                ClanEkipe clan = new ClanEkipe();
                clan.setVjencanje(vjencanje);
                clan.setSuradnik(suradnik);
                clan.setUloga(dto.uloga() != null ? dto.uloga() : suradnik.getUloga());
                clan.setHonorar(dto.honorar());
                vjencanje.getEkipa().add(clan);
            }
        }
    }

    private List<VjencanjeSazetak> uSazetke(List<Vjencanje> vjencanja) {
        if (vjencanja.isEmpty()) {
            return List.of();
        }
        List<Long> ids = vjencanja.stream().map(Vjencanje::getId).toList();

        // Dva upita za cijeli popis, umjesto upita za svako vjenčanje (N+1 problem)
        Map<Long, BigDecimal> uplacenoPoVjencanju = new HashMap<>();
        for (ZbrojPoVjencanju zbroj : uplataRepository.zbrojiPoVjencanju(ids)) {
            uplacenoPoVjencanju.put(zbroj.vjencanjeId(), zbroj.iznos());
        }
        Map<Long, String> paketi = new HashMap<>();
        for (PaketVjencanja paket : vjencanjeRepository.naziviPaketa(ids)) {
            paketi.put(paket.vjencanjeId(), paket.naziv());
        }
        Set<Long> sKaparom = new HashSet<>(uplataRepository.vjencanjaSUplatomVrste(VrstaUplate.KAPARA, ids));

        List<VjencanjeSazetak> sazeci = new ArrayList<>();
        for (Vjencanje v : vjencanja) {
            BigDecimal uplaceno = uplacenoPoVjencanju.getOrDefault(v.getId(), BigDecimal.ZERO);
            sazeci.add(new VjencanjeSazetak(
                    v.getId(),
                    v.getDatum(),
                    v.getLokacija(),
                    imeOsobe(v.getMlada()),
                    imeOsobe(v.getMladozenja()),
                    v.getStatus(),
                    v.getDogovorenaCijena(),
                    uplaceno,
                    v.getDogovorenaCijena().subtract(uplaceno),
                    sKaparom.contains(v.getId()),
                    paketi.get(v.getId())
            ));
        }
        return sazeci;
    }

    private VjencanjeDetalji uDetalje(Vjencanje v) {
        List<StavkaRasporedaDto> raspored = v.getRaspored().stream()
                .sorted(Comparator.comparing(StavkaRasporeda::getVrijeme,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(StavkaRasporedaDto::iz)
                .toList();

        List<DodatakResponse> dodaci = v.getDodaci().stream()
                .sorted(Comparator.comparing(Dodatak::getNaziv))
                .map(DodatakResponse::iz)
                .toList();

        return new VjencanjeDetalji(
                v.getId(),
                v.getDatum(),
                v.getLokacija(),
                OsobaDto.iz(v.getMlada()),
                OsobaDto.iz(v.getMladozenja()),
                v.getPaket() != null ? PaketResponse.iz(v.getPaket()) : null,
                dodaci,
                v.getStatus(),
                v.getRokIsporuke(),
                v.getIzvorUpita(),
                v.getNapomene(),
                v.getBrojGostiju(),
                v.getLinkGalerije(),
                raspored,
                v.getEkipa().stream().map(ClanEkipeResponse::iz).toList(),
                v.getUplate().stream().map(UplataResponse::iz).toList(),
                v.getZadaci().stream().map(ZadatakResponse::iz).toList(),
                financijeKalkulator.izracunaj(v)
        );
    }

    private String imeOsobe(Osoba osoba) {
        return osoba == null ? "" : osoba.punoIme();
    }

    private String parMladenaca(Vjencanje v) {
        return imeOsobe(v.getMlada()) + " i " + imeOsobe(v.getMladozenja());
    }
}
