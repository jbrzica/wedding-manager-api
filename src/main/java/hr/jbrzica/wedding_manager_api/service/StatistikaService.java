package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.StatistikaResponse;
import hr.jbrzica.wedding_manager_api.dto.StatistikaResponse.*;
import hr.jbrzica.wedding_manager_api.entity.*;
import hr.jbrzica.wedding_manager_api.repository.ClanEkipeRepository;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
import hr.jbrzica.wedding_manager_api.repository.ZbrojPoVjencanju;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StatistikaService {

    private final VjencanjeRepository vjencanjeRepository;
    private final UplataRepository uplataRepository;
    private final ClanEkipeRepository clanEkipeRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public StatistikaService(VjencanjeRepository vjencanjeRepository, UplataRepository uplataRepository,
                             ClanEkipeRepository clanEkipeRepository, TrenutniKorisnik trenutniKorisnik) {
        this.vjencanjeRepository = vjencanjeRepository;
        this.uplataRepository = uplataRepository;
        this.clanEkipeRepository = clanEkipeRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    /**
     * Uplate, honorari i dodaci zbrajaju se u bazi (GROUP BY) jednim upitom za sva vjenčanja,
     * pa je broj SQL upita isti za 10 i za 1000 vjenčanja. Prije se za svako vjenčanje
     * posebno učitavalo troje (N+1).
     */
    public StatistikaResponse za(int godina) {
        List<Vjencanje> dvijeGodine = vjencanjeRepository.zaStatistiku(
                trenutniKorisnik.tvrtkaId(), LocalDate.of(godina - 1, 1, 1), LocalDate.of(godina, 12, 31));

        List<Vjencanje> sveOve = dvijeGodine.stream().filter(v -> v.getDatum().getYear() == godina).toList();
        List<Vjencanje> posaoOve = sveOve.stream().filter(StatistikaService::jePosao).toList();
        List<Vjencanje> posaoProsle = dvijeGodine.stream()
                .filter(v -> v.getDatum().getYear() == godina - 1)
                .filter(StatistikaService::jePosao)
                .toList();

        List<Long> idsPosla = new ArrayList<>(ids(posaoOve));
        idsPosla.addAll(ids(posaoProsle));
        Map<Long, BigDecimal> uplaceno = poVjencanju(idsPosla.isEmpty() ? List.of() : uplataRepository.zbrojiPoVjencanju(idsPosla));
        Map<Long, BigDecimal> honorari = poVjencanju(idsPosla.isEmpty() ? List.of() : clanEkipeRepository.honorariPoVjencanju(idsPosla));

        return new StatistikaResponse(
                godina,
                sazetak(posaoOve, honorari),
                sazetak(posaoProsle, honorari),
                poMjesecima(posaoOve, uplaceno),
                izvori(sveOve),
                paketi(posaoOve),
                dodaci(posaoOve),
                subote(godina, sveOve),
                honorariSuradnika(posaoOve),
                upiti(sveOve),
                zaNaplatiti(posaoOve, uplaceno),
                poMjesecima(posaoProsle, uplaceno).stream().map(Mjesec::dogovoreno).toList()
        );
    }

    private static List<Long> ids(List<Vjencanje> vjencanja) {
        return vjencanja.stream().map(Vjencanje::getId).toList();
    }

    private static Map<Long, BigDecimal> poVjencanju(List<ZbrojPoVjencanju> zbrojevi) {
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (ZbrojPoVjencanju z : zbrojevi) {
            mapa.put(z.vjencanjeId(), z.iznos());
        }
        return mapa;
    }

    private Upiti upiti(List<Vjencanje> sva) {
        long posao = sva.stream().filter(StatistikaService::jePosao).count();
        long otkazano = sva.stream().filter(v -> v.getStatus() == StatusVjencanja.OTKAZANO).count();
        long otvoreno = sva.stream().filter(v -> v.getStatus() == StatusVjencanja.UPIT).count();
        return new Upiti(sva.size(), posao, otkazano, otvoreno);
    }

    // Koliko od dogovorenog posla te godine još nije uplaćeno
    private BigDecimal zaNaplatiti(List<Vjencanje> posao, Map<Long, BigDecimal> uplaceno) {
        BigDecimal ukupno = BigDecimal.ZERO;
        for (Vjencanje v : posao) {
            BigDecimal preostalo = v.getDogovorenaCijena().subtract(uplaceno.getOrDefault(v.getId(), BigDecimal.ZERO));
            if (preostalo.signum() > 0) {
                ukupno = ukupno.add(preostalo);
            }
        }
        return ukupno;
    }

    // Upit i otkazano vjenčanje nisu dogovoreni posao
    private static boolean jePosao(Vjencanje v) {
        return v.getStatus() != StatusVjencanja.UPIT && v.getStatus() != StatusVjencanja.OTKAZANO;
    }

    private SazetakGodine sazetak(List<Vjencanje> vjencanja, Map<Long, BigDecimal> honorariPoVjencanju) {
        BigDecimal prihod = BigDecimal.ZERO;
        BigDecimal honorari = BigDecimal.ZERO;
        for (Vjencanje v : vjencanja) {
            prihod = prihod.add(v.getDogovorenaCijena());
            honorari = honorari.add(honorariPoVjencanju.getOrDefault(v.getId(), BigDecimal.ZERO));
            if (v.getOstaliTroskovi() != null) {
                honorari = honorari.add(v.getOstaliTroskovi());
            }
        }
        BigDecimal prosjek = vjencanja.isEmpty()
                ? BigDecimal.ZERO
                : prihod.divide(BigDecimal.valueOf(vjencanja.size()), 2, RoundingMode.HALF_UP);
        return new SazetakGodine(vjencanja.size(), prihod, prihod.subtract(honorari), prosjek);
    }

    private List<Mjesec> poMjesecima(List<Vjencanje> vjencanja, Map<Long, BigDecimal> uplacenoPoVjencanju) {
        List<Mjesec> mjeseci = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            long broj = 0;
            BigDecimal dogovoreno = BigDecimal.ZERO;
            BigDecimal uplaceno = BigDecimal.ZERO;
            for (Vjencanje v : vjencanja) {
                if (v.getDatum().getMonthValue() == m) {
                    broj++;
                    dogovoreno = dogovoreno.add(v.getDogovorenaCijena());
                    uplaceno = uplaceno.add(uplacenoPoVjencanju.getOrDefault(v.getId(), BigDecimal.ZERO));
                }
            }
            mjeseci.add(new Mjesec(m, broj, dogovoreno, uplaceno));
        }
        return mjeseci;
    }

    private List<Izvor> izvori(List<Vjencanje> sva) {
        // LinkedHashMap čuva redoslijed; null ključ = izvor nije upisan
        Map<IzvorUpita, long[]> brojevi = new LinkedHashMap<>();
        for (Vjencanje v : sva) {
            long[] b = brojevi.computeIfAbsent(v.getIzvorUpita(), k -> new long[2]);
            b[0]++;
            if (jePosao(v)) {
                b[1]++;
            }
        }
        return brojevi.entrySet().stream()
                .map(e -> new Izvor(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(Izvor::upita).reversed())
                .toList();
    }

    private List<Stavka> paketi(List<Vjencanje> vjencanja) {
        Map<String, Long> brojevi = new HashMap<>();
        for (Vjencanje v : vjencanja) {
            String naziv = v.getPaket() != null ? v.getPaket().getNaziv() : "Bez paketa";
            brojevi.merge(naziv, 1L, Long::sum);
        }
        return uStavke(brojevi);
    }

    private List<Stavka> dodaci(List<Vjencanje> vjencanja) {
        if (vjencanja.isEmpty()) {
            return List.of();
        }
        return vjencanjeRepository.dodaciPoNazivu(ids(vjencanja)).stream()
                .map(d -> new Stavka(d.naziv(), d.broj()))
                .toList();
    }

    private List<Stavka> uStavke(Map<String, Long> brojevi) {
        return brojevi.entrySet().stream()
                .map(e -> new Stavka(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Stavka::broj).reversed())
                .toList();
    }

    // Sve subote od svibnja do rujna: koliko je na njima dogovorenih vjenčanja, a koliko otvorenih upita
    private List<Subota> subote(int godina, List<Vjencanje> sva) {
        Map<LocalDate, long[]> poDatumu = new HashMap<>();
        for (Vjencanje v : sva) {
            long[] b = poDatumu.computeIfAbsent(v.getDatum(), k -> new long[2]);
            if (jePosao(v)) {
                b[0]++;
            } else if (v.getStatus() == StatusVjencanja.UPIT) {
                b[1]++;
            }
        }

        List<Subota> subote = new ArrayList<>();
        LocalDate dan = LocalDate.of(godina, 5, 1);
        while (dan.getDayOfWeek() != DayOfWeek.SATURDAY) {
            dan = dan.plusDays(1);
        }
        LocalDate kraj = LocalDate.of(godina, 9, 30);
        for (; !dan.isAfter(kraj); dan = dan.plusWeeks(1)) {
            long[] b = poDatumu.getOrDefault(dan, new long[2]);
            subote.add(new Subota(dan, b[0], b[1]));
        }
        return subote;
    }

    private List<Honorar> honorariSuradnika(List<Vjencanje> vjencanja) {
        if (vjencanja.isEmpty()) {
            return List.of();
        }
        return clanEkipeRepository.honorariPoSuradniku(ids(vjencanja)).stream()
                .map(h -> new Honorar(h.ime(), h.brojVjencanja(), h.ukupno()))
                .toList();
    }
}
