package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.DugResponse;
import hr.jbrzica.wedding_manager_api.dto.UplataRequest;
import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UplataService {

    private final UplataRepository uplataRepository;
    private final VjencanjeService vjencanjeService;

    public UplataService(UplataRepository uplataRepository, VjencanjeService vjencanjeService) {
        this.uplataRepository = uplataRepository;
        this.vjencanjeService = vjencanjeService;
    }

    public List<Uplata> dohvatiZaVjencanje(Long vjencanjeId) {
        vjencanjeService.dohvatiPoId(vjencanjeId);   // baci 404 ako vjenčanje ne postoji
        return uplataRepository.findByVjencanjeId(vjencanjeId);
    }

    public Uplata stvori(Long vjencanjeId, UplataRequest request) {
        Vjencanje vjencanje = vjencanjeService.dohvatiPoId(vjencanjeId);

        Uplata uplata = new Uplata();
        uplata.setIznos(request.iznos());
        uplata.setDatum(request.datum());
        uplata.setOpis(request.opis());
        uplata.setVjencanje(vjencanje);

        return uplataRepository.save(uplata);
    }

    public DugResponse izracunajDug(Long vjencanjeId) {
        Vjencanje vjencanje = vjencanjeService.dohvatiPoId(vjencanjeId);

        BigDecimal ukupnaCijena = vjencanje.getPaket().getCijena();
        for (Dodatak dodatak : vjencanje.getDodaci()) {
            ukupnaCijena = ukupnaCijena.add(dodatak.getCijena());
        }

        BigDecimal uplaceno = BigDecimal.ZERO;
        for (Uplata uplata : uplataRepository.findByVjencanjeId(vjencanjeId)) {
            uplaceno = uplaceno.add(uplata.getIznos());
        }

        BigDecimal dug = ukupnaCijena.subtract(uplaceno);

        return new DugResponse(ukupnaCijena, uplaceno, dug);
    }
}