package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.VjencanjeRequest;
import hr.jbrzica.wedding_manager_api.entity.*;
import hr.jbrzica.wedding_manager_api.repository.VjencanjeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VjencanjeService {

    private final VjencanjeRepository vjencanjeRepository;
    private final KlijentService klijentService;
    private final PaketService paketService;
    private final DodatakService dodatakService;

    public VjencanjeService(VjencanjeRepository vjencanjeRepository,
                            KlijentService klijentService,
                            PaketService paketService,
                            DodatakService dodatakService) {
        this.vjencanjeRepository = vjencanjeRepository;
        this.klijentService = klijentService;
        this.paketService = paketService;
        this.dodatakService = dodatakService;
    }

    public List<Vjencanje> dohvatiSve(StatusVjencanja status) {
        if (status == null) {
            return vjencanjeRepository.findAll();
        }
        return vjencanjeRepository.findByStatus(status);
    }

    public Vjencanje dohvatiPoId(Long id) {
        return vjencanjeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vjencanje s id " + id + " ne postoji"));
    }

    public Vjencanje stvori(VjencanjeRequest request) {
        Klijent klijent = klijentService.dohvatiPoId(request.klijentId());
        Paket paket = paketService.dohvatiPoId(request.paketId());

        Vjencanje vjencanje = new Vjencanje();
        vjencanje.setDatum(request.datum());
        vjencanje.setLokacija(request.lokacija());
        vjencanje.setKlijent(klijent);
        vjencanje.setPaket(paket);

        return vjencanjeRepository.save(vjencanje);
    }

    public Vjencanje promijeniStatus(Long id, StatusVjencanja noviStatus) {
        Vjencanje vjencanje = dohvatiPoId(id);
        vjencanje.setStatus(noviStatus);
        return vjencanjeRepository.save(vjencanje);
    }

    public Vjencanje dodajDodatak(Long vjencanjeId, Long dodatakId) {
        Vjencanje vjencanje = dohvatiPoId(vjencanjeId);
        Dodatak dodatak = dodatakService.dohvatiPoId(dodatakId);
        vjencanje.getDodaci().add(dodatak);
        return vjencanjeRepository.save(vjencanje);
    }

    public void obrisi(Long id) {
        Vjencanje vjencanje = dohvatiPoId(id);
        vjencanjeRepository.delete(vjencanje);
    }
}