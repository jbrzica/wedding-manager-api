package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.DodatakRequest;
import hr.jbrzica.wedding_manager_api.dto.DodatakResponse;
import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.repository.DodatakRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DodatakService {

    private final DodatakRepository dodatakRepository;
    private final TvrtkaRepository tvrtkaRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public DodatakService(DodatakRepository dodatakRepository, TvrtkaRepository tvrtkaRepository,
            TrenutniKorisnik trenutniKorisnik) {
        this.dodatakRepository = dodatakRepository;
        this.tvrtkaRepository = tvrtkaRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    public List<DodatakResponse> dohvatiSve() {
        return dodatakRepository.findByTvrtkaIdOrderByNazivAsc(trenutniKorisnik.tvrtkaId()).stream()
                .map(DodatakResponse::iz)
                .toList();
    }

    public DodatakResponse dohvati(Long id) {
        return DodatakResponse.iz(dohvatiEntitet(id));
    }

    @Transactional
    public DodatakResponse stvori(DodatakRequest request) {
        Dodatak dodatak = new Dodatak();
        dodatak.setTvrtka(tvrtkaRepository.getReferenceById(trenutniKorisnik.tvrtkaId()));
        popuni(dodatak, request);
        return DodatakResponse.iz(dodatakRepository.save(dodatak));
    }

    @Transactional
    public DodatakResponse izmijeni(Long id, DodatakRequest request) {
        Dodatak dodatak = dohvatiEntitet(id);
        popuni(dodatak, request);
        return DodatakResponse.iz(dodatak);
    }

    @Transactional
    public void obrisi(Long id) {
        dodatakRepository.delete(dohvatiEntitet(id));
        dodatakRepository.flush();
    }

    public Dodatak dohvatiEntitet(Long id) {
        return dodatakRepository.findByIdAndTvrtkaId(id, trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dodatak s id " + id + " ne postoji"));
    }

    // Dohvati sve tražene dodatke; ako ijedan ne postoji, 404.
    public List<Dodatak> dohvatiEntitete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> jedinstveni = new HashSet<>(ids);
        List<Dodatak> dodaci = dodatakRepository.findByIdInAndTvrtkaId(jedinstveni, trenutniKorisnik.tvrtkaId());
        if (dodaci.size() != jedinstveni.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Neki od odabranih dodataka ne postoje");
        }
        return dodaci;
    }

    private void popuni(Dodatak dodatak, DodatakRequest request) {
        dodatak.setNaziv(request.naziv());
        dodatak.setCijena(request.cijena());
    }
}
