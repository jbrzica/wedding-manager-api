package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.KosStavka;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Koš za smeće: obrisana vjenčanja čekaju 30 dana, pa se brišu zauvijek.
 * Štiti od krivog klika, jer je noćni backup star i do 24 sata.
 */
@Service
public class KosService {

    static final Duration ROK = Duration.ofDays(30);
    private static final Logger log = LoggerFactory.getLogger(KosService.class);

    private final VjencanjeRepository vjencanjeRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public KosService(VjencanjeRepository vjencanjeRepository, TrenutniKorisnik trenutniKorisnik) {
        this.vjencanjeRepository = vjencanjeRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @Transactional(readOnly = true)
    public List<KosStavka> popis() {
        return vjencanjeRepository.uKosu(trenutniKorisnik.tvrtkaId()).stream()
                .map(v -> KosStavka.iz(v, v.getObrisano().plus(ROK)))
                .toList();
    }

    @Transactional
    public void vrati(Long id) {
        if (vjencanjeRepository.vratiIzKosa(id, trenutniKorisnik.tvrtkaId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vjenčanje nije u košu");
        }
    }

    @Transactional
    public void obrisiTrajno(Long id) {
        if (vjencanjeRepository.obrisiTrajno(id, trenutniKorisnik.tvrtkaId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vjenčanje nije u košu");
        }
    }

    // Svaku noć, nakon backupa u 4:00, brišu se vjenčanja koja su u košu dulje od 30 dana.
    @Scheduled(cron = "0 30 4 * * *", zone = "Europe/Zagreb")
    @Transactional
    public void isprazniStaro() {
        int obrisano = vjencanjeRepository.obrisiStarijaOd(Instant.now().minus(ROK));
        if (obrisano > 0) {
            log.info("Iz koša trajno obrisano vjenčanja: {}", obrisano);
        }
    }
}
