package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.PaketRequest;
import hr.jbrzica.wedding_manager_api.dto.PaketResponse;
import hr.jbrzica.wedding_manager_api.entity.Paket;
import hr.jbrzica.wedding_manager_api.repository.PaketRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PaketService {

    private final PaketRepository paketRepository;
    private final TvrtkaRepository tvrtkaRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public PaketService(PaketRepository paketRepository, TvrtkaRepository tvrtkaRepository,
            TrenutniKorisnik trenutniKorisnik) {
        this.paketRepository = paketRepository;
        this.tvrtkaRepository = tvrtkaRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    public List<PaketResponse> dohvatiSve() {
        return paketRepository.findByTvrtkaIdOrderByNazivAsc(trenutniKorisnik.tvrtkaId()).stream()
                .map(PaketResponse::iz)
                .toList();
    }

    public PaketResponse dohvati(Long id) {
        return PaketResponse.iz(dohvatiEntitet(id));
    }

    @Transactional
    public PaketResponse stvori(PaketRequest request) {
        Paket paket = new Paket();
        paket.setTvrtka(tvrtkaRepository.getReferenceById(trenutniKorisnik.tvrtkaId()));
        popuni(paket, request);
        return PaketResponse.iz(paketRepository.save(paket));
    }

    @Transactional
    public PaketResponse izmijeni(Long id, PaketRequest request) {
        Paket paket = dohvatiEntitet(id);
        popuni(paket, request);
        return PaketResponse.iz(paket);
    }

    @Transactional
    public void obrisi(Long id) {
        paketRepository.delete(dohvatiEntitet(id));
        // flush odmah, da greška (paket se koristi na vjenčanju) pukne ovdje i postane 409
        paketRepository.flush();
    }

    public Paket dohvatiEntitet(Long id) {
        return paketRepository.findByIdAndTvrtkaId(id, trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Paket s id " + id + " ne postoji"));
    }

    private void popuni(Paket paket, PaketRequest request) {
        paket.setNaziv(request.naziv());
        paket.setOpis(request.opis());
        paket.setCijena(request.cijena());
    }
}
