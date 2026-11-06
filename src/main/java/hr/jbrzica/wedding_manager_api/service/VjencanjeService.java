package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.entity.*;
import hr.jbrzica.wedding_manager_api.repository.ClanEkipeRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
import hr.jbrzica.wedding_manager_api.repository.ZbrojUplata;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public VjencanjeService(VjencanjeRepository vjencanjeRepository,
                            UplataRepository uplataRepository,
                            ClanEkipeRepository clanEkipeRepository,
                            PaketService paketService,
                            DodatakService dodatakService,
                            SuradnikService suradnikService,
                            FinancijeKalkulator financijeKalkulator,
                            TvrtkaRepository tvrtkaRepository,
                            TrenutniKorisnik trenutniKorisnik) {
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

    public List<VjencanjeSazetak> popis(Razdoblje razdoblje, Integer godina, List<StatusVjencanja> statusi,
                                        String pretraga, boolean nenaplaceno, boolean kasniIsporuka) {
        LocalDate danas = LocalDate.now();
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();
        List<Vjencanje> vjencanja = switch (razdoblje) {
            case NADOLAZECA -> vjencanjeRepository.findByTvrtkaIdAndDatumGreaterThanEqualOrderByDatumAsc(tvrtkaId, danas);
            case PROSLA -> vjencanjeRepository.findByTvrtkaIdAndDatumLessThanOrderByDatumDesc(tvrtkaId, danas);
            case SVA -> vjencanjeRepository.findByTvrtkaIdOrderByDatumAsc(tvrtkaId);
        };

        // Jedan fotograf ima stotinjak vjenčanja godišnje, pa je filtriranje u Javi dovoljno brzo.
        List<Vjencanje> filtrirana = vjencanja.stream()
                .filter(v -> godina == null || v.getDatum().getYear() == godina)
                .filter(v -> statusi == null || statusi.isEmpty() || statusi.contains(v.getStatus()))
                .filter(v -> odgovaraPretrazi(v, pretraga))
                .filter(v -> !kasniIsporuka || kasniIsporuka(v, danas))
                .toList();

        List<VjencanjeSazetak> sazeci = uSazetke(filtrirana);
        // Preostali iznos postoji tek u sažetku, jer se uplate zbrajaju jednim upitom za cijeli popis
        if (nenaplaceno) {
            return sazeci.stream()
                    .filter(s -> s.status() != StatusVjencanja.OTKAZANO && s.preostalo().signum() > 0)
                    .toList();
        }
        return sazeci;
    }

    // Vjenčanje je bilo, rok je prošao, a fotografije još nisu predane. Otkazana vjenčanja ne kasne.
    private boolean kasniIsporuka(Vjencanje v, LocalDate danas) {
        return v.getDatum().isBefore(danas)
                && v.getRokIsporuke() != null
                && v.getRokIsporuke().isBefore(danas)
                && v.getStatus() != StatusVjencanja.ISPORUCENO
                && v.getStatus() != StatusVjencanja.OTKAZANO;
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

        return new PregledResponse(sljedeceSazetak, danaDoSljedeceg, brojOveGodine, ukupnoNenaplaceno);
    }

    public UpozorenjaResponse provjeriPreklapanja(LocalDate datum, List<Long> suradnikIds, Long izuzmiId) {
        List<String> upozorenja = new ArrayList<>();
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();

        for (Vjencanje v : vjencanjeRepository.findByTvrtkaIdAndDatum(tvrtkaId, datum)) {
            if (!v.getId().equals(izuzmiId) && v.getStatus() != StatusVjencanja.OTKAZANO) {
                upozorenja.add("Na taj datum već imate vjenčanje: " + parMladenaca(v));
            }
        }

        if (suradnikIds != null && !suradnikIds.isEmpty()) {
            for (ClanEkipe clan : clanEkipeRepository.findBySuradnikIdInAndVjencanjeDatumAndVjencanjeTvrtkaId(
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
        return uDetalje(vjencanjeRepository.save(vjencanje));
    }

    @Transactional
    public VjencanjeDetalji izmijeni(Long id, VjencanjeRequest request) {
        Vjencanje vjencanje = dohvatiEntitet(id);
        popuni(vjencanje, request);
        return uDetalje(vjencanje);
    }

    @Transactional
    public VjencanjeDetalji promijeniStatus(Long id, StatusVjencanja status) {
        Vjencanje vjencanje = dohvatiEntitet(id);
        vjencanje.setStatus(status);
        return uDetalje(vjencanje);
    }

    @Transactional
    public void obrisi(Long id) {
        // raspored, ekipa i uplate brišu se zajedno s vjenčanjem (cascade)
        vjencanjeRepository.delete(dohvatiEntitet(id));
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
        vjencanje.setLinkGalerije(request.linkGalerije());

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
        for (ZbrojUplata zbroj : uplataRepository.zbrojiPoVjencanju(ids)) {
            uplacenoPoVjencanju.put(zbroj.vjencanjeId(), zbroj.iznos());
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
                    sKaparom.contains(v.getId())
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

    private boolean odgovaraPretrazi(Vjencanje v, String pretraga) {
        if (pretraga == null || pretraga.isBlank()) {
            return true;
        }
        String trazeno = pretraga.trim().toLowerCase();
        return sadrzi(imeOsobe(v.getMlada()), trazeno)
                || sadrzi(imeOsobe(v.getMladozenja()), trazeno)
                || sadrzi(v.getLokacija(), trazeno);
    }

    private boolean sadrzi(String tekst, String trazeno) {
        return tekst != null && tekst.toLowerCase().contains(trazeno);
    }

    private String imeOsobe(Osoba osoba) {
        return osoba == null ? "" : osoba.punoIme();
    }

    private String parMladenaca(Vjencanje v) {
        return imeOsobe(v.getMlada()) + " i " + imeOsobe(v.getMladozenja());
    }
}
