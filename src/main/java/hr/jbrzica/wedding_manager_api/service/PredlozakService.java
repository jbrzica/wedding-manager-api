package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.StavkaRasporedaDto;
import hr.jbrzica.wedding_manager_api.entity.PredlozakStavka;
import hr.jbrzica.wedding_manager_api.repository.PredlozakStavkaRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Predložak rasporeda: tvrtka jednom spremi svoj tipični dan, a svako novo vjenčanje kreće od njega.
 */
@Service
public class PredlozakService {

    private final PredlozakStavkaRepository predlozakRepository;
    private final TvrtkaRepository tvrtkaRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public PredlozakService(PredlozakStavkaRepository predlozakRepository, TvrtkaRepository tvrtkaRepository,
                            TrenutniKorisnik trenutniKorisnik) {
        this.predlozakRepository = predlozakRepository;
        this.tvrtkaRepository = tvrtkaRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @Transactional(readOnly = true)
    public List<StavkaRasporedaDto> dohvati() {
        return predlozakRepository.findByTvrtkaIdOrderByRedoslijedAsc(trenutniKorisnik.tvrtkaId()).stream()
                .map(s -> new StavkaRasporedaDto(s.getVrijeme(), s.getNaziv(), s.getAdresa(), s.getNapomena()))
                .toList();
    }

    // Novi predložak u cijelosti zamjenjuje stari; prazan popis briše predložak
    @Transactional
    public List<StavkaRasporedaDto> spremi(List<StavkaRasporedaDto> stavke) {
        Long tvrtkaId = trenutniKorisnik.tvrtkaId();
        predlozakRepository.deleteByTvrtkaId(tvrtkaId);
        predlozakRepository.flush();

        List<PredlozakStavka> nove = new ArrayList<>();
        for (int i = 0; i < stavke.size(); i++) {
            StavkaRasporedaDto dto = stavke.get(i);
            PredlozakStavka s = new PredlozakStavka();
            s.setTvrtka(tvrtkaRepository.getReferenceById(tvrtkaId));
            s.setRedoslijed(i);
            s.setVrijeme(dto.vrijeme());
            s.setNaziv(dto.naziv());
            s.setAdresa(dto.adresa());
            s.setNapomena(dto.napomena());
            nove.add(s);
        }
        predlozakRepository.saveAll(nove);
        return dohvati();
    }
}
