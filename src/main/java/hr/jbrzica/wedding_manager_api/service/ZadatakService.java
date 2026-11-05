package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.ZadatakRequest;
import hr.jbrzica.wedding_manager_api.dto.ZadatakResponse;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.Zadatak;
import hr.jbrzica.wedding_manager_api.repository.ZadatakRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ZadatakService {

    private final ZadatakRepository zadatakRepository;
    private final VjencanjeService vjencanjeService;

    public ZadatakService(ZadatakRepository zadatakRepository, VjencanjeService vjencanjeService) {
        this.zadatakRepository = zadatakRepository;
        this.vjencanjeService = vjencanjeService;
    }

    public ZadatakResponse dodaj(Long vjencanjeId, ZadatakRequest request) {
        // dohvatiEntitet provjerava da je vjenčanje od korisnikove tvrtke
        Vjencanje vjencanje = vjencanjeService.dohvatiEntitet(vjencanjeId);
        Zadatak zadatak = new Zadatak();
        zadatak.setNaziv(request.naziv().trim());
        zadatak.setRedoslijed(vjencanje.getZadaci().size());
        zadatak.setVjencanje(vjencanje);
        return ZadatakResponse.iz(zadatakRepository.save(zadatak));
    }

    public ZadatakResponse oznaci(Long vjencanjeId, Long zadatakId, boolean gotovo) {
        Zadatak zadatak = dohvati(vjencanjeId, zadatakId);
        zadatak.setGotovo(gotovo);
        return ZadatakResponse.iz(zadatak);
    }

    public void obrisi(Long vjencanjeId, Long zadatakId) {
        zadatakRepository.delete(dohvati(vjencanjeId, zadatakId));
    }

    private Zadatak dohvati(Long vjencanjeId, Long zadatakId) {
        vjencanjeService.dohvatiEntitet(vjencanjeId);
        return zadatakRepository.findByIdAndVjencanjeId(zadatakId, vjencanjeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zadatak ne postoji"));
    }
}
