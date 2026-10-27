package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.repository.DodatakRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DodatakService {

    private final DodatakRepository dodatakRepository;

    public DodatakService(DodatakRepository dodatakRepository) {
        this.dodatakRepository = dodatakRepository;
    }

    public List<Dodatak> dohvatiSve() {
        return dodatakRepository.findAll();
    }

    public Dodatak dohvatiPoId(Long id) {
        return dodatakRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dodatak s id " + id + " ne postoji"));
    }

    public Dodatak spremi(Dodatak dodatak) {
        return dodatakRepository.save(dodatak);
    }

    public void obrisi(Long id) {
        Dodatak dodatak = dohvatiPoId(id);
        dodatakRepository.delete(dodatak);
    }
}