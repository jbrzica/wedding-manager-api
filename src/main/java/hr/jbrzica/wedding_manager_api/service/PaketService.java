package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.entity.Paket;
import hr.jbrzica.wedding_manager_api.repository.PaketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PaketService {

    private final PaketRepository paketRepository;

    public PaketService(PaketRepository paketRepository) {
        this.paketRepository = paketRepository;
    }

    public List<Paket> dohvatiSve() {
        return paketRepository.findAll();
    }

    public Paket dohvatiPoId(Long id) {
        return paketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Paket s id " + id + " ne postoji"));

    }

    public Paket spremi(Paket paket) {
        return paketRepository.save(paket);
    }

    public void obrisi(Long id) {
        Paket paket = dohvatiPoId(id);
        paketRepository.delete(paket);
    }
}