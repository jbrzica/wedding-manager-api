package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.UplataRequest;
import hr.jbrzica.wedding_manager_api.dto.UplataResponse;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UplataService {

    private final UplataRepository uplataRepository;
    private final VjencanjeService vjencanjeService;

    public UplataService(UplataRepository uplataRepository, VjencanjeService vjencanjeService) {
        this.uplataRepository = uplataRepository;
        this.vjencanjeService = vjencanjeService;
    }

    public List<UplataResponse> dohvatiZaVjencanje(Long vjencanjeId) {
        vjencanjeService.dohvatiEntitet(vjencanjeId);   // baci 404 ako vjenčanje ne postoji
        return uplataRepository.findByVjencanjeIdOrderByDatumAsc(vjencanjeId).stream()
                .map(UplataResponse::iz)
                .toList();
    }

    @Transactional
    public UplataResponse stvori(Long vjencanjeId, UplataRequest request) {
        Vjencanje vjencanje = vjencanjeService.dohvatiEntitet(vjencanjeId);

        Uplata uplata = new Uplata();
        uplata.setIznos(request.iznos());
        uplata.setDatum(request.datum());
        uplata.setVrsta(request.vrsta());
        uplata.setOpis(request.opis());
        uplata.setVjencanje(vjencanje);
        vjencanje.getUplate().add(uplata);

        // Plaćena kapara znači da je termin potvrđen.
        if (request.vrsta() == VrstaUplate.KAPARA && vjencanje.getStatus() == StatusVjencanja.UPIT) {
            vjencanje.setStatus(StatusVjencanja.POTVRDENO);
        }

        return UplataResponse.iz(uplataRepository.save(uplata));
    }

    @Transactional
    public void obrisi(Long vjencanjeId, Long uplataId) {
        vjencanjeService.dohvatiEntitet(vjencanjeId);   // provjerava da je vjenčanje od moje tvrtke
        Uplata uplata = uplataRepository.findById(uplataId)
                .filter(u -> u.getVjencanje().getId().equals(vjencanjeId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Uplata s id " + uplataId + " ne postoji na ovom vjenčanju"));
        // orphanRemoval obriše uplatu kad je maknemo iz liste vjenčanja
        uplata.getVjencanje().getUplate().remove(uplata);
    }
}
