package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.StatistikaResponse;
import hr.jbrzica.wedding_manager_api.dto.StatistikaResponse.*;
import hr.jbrzica.wedding_manager_api.entity.*;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
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
    private final TrenutniKorisnik trenutniKorisnik;

    public StatistikaService(VjencanjeRepository vjencanjeRepository, TrenutniKorisnik trenutniKorisnik) {
        this.vjencanjeRepository = vjencanjeRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    // Fotograf ima stotinjak vjenčanja godišnje, pa se sve računa u Javi iz jednog upita
    public StatistikaResponse za(int godina) {
        List<Vjencanje> dvijeGodine = vjencanjeRepository.findByTvrtkaIdAndDatumBetweenOrderByDatumAsc(
                trenutniKorisnik.tvrtkaId(), LocalDate.of(godina - 1, 1, 1), LocalDate.of(godina, 12, 31));

        List<Vjencanje> sveOve = dvijeGodine.stream().filter(v -> v.getDatum().getYear() == godina).toList();
        List<Vjencanje> posaoOve = sveOve.stream().filter(StatistikaService::jePosao).toList();
        List<Vjencanje> posaoProsle = dvijeGodine.stream()
                .filter(v -> v.getDatum().getYear() == godina - 1)
                .filter(StatistikaService::jePosao)
                .toList();

        return new StatistikaResponse(
                godina,
                sazetak(posaoOve),
                sazetak(posaoProsle),
                poMjesecima(posaoOve),
                izvori(sveOve),
                paketi(posaoOve),
                dodaci(posaoOve),
                subote(godina, sveOve),
                honorari(posaoOve)
        );
    }

    // Upit i otkazano vjenčanje nisu dogovoreni posao
    private static boolean jePosao(Vjencanje v) {
        return v.getStatus() != StatusVjencanja.UPIT && v.getStatus() != StatusVjencanja.OTKAZANO;
    }

    private SazetakGodine sazetak(List<Vjencanje> vjencanja) {
        BigDecimal prihod = BigDecimal.ZERO;
        BigDecimal honorari = BigDecimal.ZERO;
        for (Vjencanje v : vjencanja) {
            prihod = prihod.add(v.getDogovorenaCijena());
            honorari = honorari.add(honorari(v));
            if (v.getOstaliTroskovi() != null) {
                honorari = honorari.add(v.getOstaliTroskovi());
            }
        }
        BigDecimal prosjek = vjencanja.isEmpty()
                ? BigDecimal.ZERO
                : prihod.divide(BigDecimal.valueOf(vjencanja.size()), 2, RoundingMode.HALF_UP);
        return new SazetakGodine(vjencanja.size(), prihod, prihod.subtract(honorari), prosjek);
    }

    private List<Mjesec> poMjesecima(List<Vjencanje> vjencanja) {
        List<Mjesec> mjeseci = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            long broj = 0;
            BigDecimal dogovoreno = BigDecimal.ZERO;
            BigDecimal uplaceno = BigDecimal.ZERO;
            for (Vjencanje v : vjencanja) {
                if (v.getDatum().getMonthValue() == m) {
                    broj++;
                    dogovoreno = dogovoreno.add(v.getDogovorenaCijena());
                    for (Uplata u : v.getUplate()) {
                        uplaceno = uplaceno.add(u.getIznos());
                    }
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
        Map<String, Long> brojevi = new HashMap<>();
        for (Vjencanje v : vjencanja) {
            for (Dodatak d : v.getDodaci()) {
                brojevi.merge(d.getNaziv(), 1L, Long::sum);
            }
        }
        return uStavke(brojevi);
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

    private List<Honorar> honorari(List<Vjencanje> vjencanja) {
        Map<String, Honorar> poSuradniku = new HashMap<>();
        for (Vjencanje v : vjencanja) {
            for (ClanEkipe c : v.getEkipa()) {
                BigDecimal iznos = c.getHonorar() != null ? c.getHonorar() : BigDecimal.ZERO;
                poSuradniku.merge(c.getSuradnik().getIme(),
                        new Honorar(c.getSuradnik().getIme(), 1, iznos),
                        (a, b) -> new Honorar(a.ime(), a.brojVjencanja() + 1, a.ukupno().add(b.ukupno())));
            }
        }
        return poSuradniku.values().stream()
                .sorted(Comparator.comparing(Honorar::ukupno).reversed())
                .toList();
    }

    private BigDecimal honorari(Vjencanje v) {
        BigDecimal zbroj = BigDecimal.ZERO;
        for (ClanEkipe c : v.getEkipa()) {
            if (c.getHonorar() != null) {
                zbroj = zbroj.add(c.getHonorar());
            }
        }
        return zbroj;
    }
}
