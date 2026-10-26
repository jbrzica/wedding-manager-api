package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.entity.Klijent;
import hr.jbrzica.wedding_manager_api.repository.KlijentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class KlijentService {

    private final KlijentRepository klijentRepository;

    public KlijentService(KlijentRepository klijentRepository) {
        this.klijentRepository = klijentRepository;
    }

    public List<Klijent> dohvatiSve() {
        return klijentRepository.findAll();
    }

    public Klijent dohvatiPoId(Long id) {
        return klijentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Klijent s id " + id + " ne postoji"));
    }

    public Klijent spremi(Klijent klijent) {
        return klijentRepository.save(klijent);
    }

    public void obrisi(Long id) {
        Klijent klijent = dohvatiPoId(id);
        klijentRepository.delete(klijent);
    }
}