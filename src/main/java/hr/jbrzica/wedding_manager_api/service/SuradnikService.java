package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.SuradnikRequest;
import hr.jbrzica.wedding_manager_api.dto.SuradnikResponse;
import hr.jbrzica.wedding_manager_api.entity.Suradnik;
import hr.jbrzica.wedding_manager_api.repository.SuradnikRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SuradnikService {

    private final SuradnikRepository suradnikRepository;

    public SuradnikService(SuradnikRepository suradnikRepository) {
        this.suradnikRepository = suradnikRepository;
    }

    public List<SuradnikResponse> dohvatiSve() {
        return suradnikRepository.findAllByOrderByImeAsc().stream()
                .map(SuradnikResponse::iz)
                .toList();
    }

    public SuradnikResponse dohvati(Long id) {
        return SuradnikResponse.iz(dohvatiEntitet(id));
    }

    @Transactional
    public SuradnikResponse stvori(SuradnikRequest request) {
        Suradnik suradnik = new Suradnik();
        popuni(suradnik, request);
        return SuradnikResponse.iz(suradnikRepository.save(suradnik));
    }

    @Transactional
    public SuradnikResponse izmijeni(Long id, SuradnikRequest request) {
        Suradnik suradnik = dohvatiEntitet(id);
        popuni(suradnik, request);
        return SuradnikResponse.iz(suradnik);
    }

    @Transactional
    public void obrisi(Long id) {
        suradnikRepository.delete(dohvatiEntitet(id));
        suradnikRepository.flush();
    }

    public Suradnik dohvatiEntitet(Long id) {
        return suradnikRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Suradnik s id " + id + " ne postoji"));
    }

    private void popuni(Suradnik suradnik, SuradnikRequest request) {
        suradnik.setIme(request.ime());
        suradnik.setUloga(request.uloga());
        suradnik.setTelefon(request.telefon());
        suradnik.setEmail(request.email());
        suradnik.setNapomena(request.napomena());
    }
}
