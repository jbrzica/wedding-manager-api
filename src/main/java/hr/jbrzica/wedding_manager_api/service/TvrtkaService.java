package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.entity.SvrhaKoda;
import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.security.KorisnikPrincipal;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TvrtkaService {

    private final TvrtkaRepository tvrtkaRepository;
    private final KorisnikRepository korisnikRepository;
    private final KodServis kodServis;
    private final TrenutniKorisnik trenutniKorisnik;

    public TvrtkaService(TvrtkaRepository tvrtkaRepository, KorisnikRepository korisnikRepository,
                         KodServis kodServis, TrenutniKorisnik trenutniKorisnik) {
        this.tvrtkaRepository = tvrtkaRepository;
        this.korisnikRepository = korisnikRepository;
        this.kodServis = kodServis;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    public TvrtkaResponse moja() {
        Tvrtka tvrtka = mojaTvrtka();
        List<ClanTvrtkeResponse> clanovi = korisnikRepository.findByTvrtkaIdOrderByImeAsc(tvrtka.getId()).stream()
                .map(ClanTvrtkeResponse::iz)
                .toList();
        return new TvrtkaResponse(tvrtka.getId(), tvrtka.getNaziv(), clanovi);
    }

    @Transactional
    public TvrtkaResponse preimenuj(TvrtkaRequest request) {
        provjeriVlasnika();
        mojaTvrtka().setNaziv(request.naziv());
        return moja();
    }

    @Transactional
    public PorukaResponse pozovi(EmailRequest request) {
        provjeriVlasnika();
        String email = AuthService.normaliziraj(request.email());
        if (korisnikRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Korisnik s tim emailom već ima račun");
        }

        Tvrtka tvrtka = mojaTvrtka();
        kodServis.posaljiKod(email, SvrhaKoda.POZIV, tvrtka,
                "Poziv u " + tvrtka.getNaziv() + " – Wedding Manager",
                "Pozvani ste da se pridružite tvrtki " + tvrtka.getNaziv() + " u aplikaciji Wedding Manager. "
                        + "U aplikaciji odaberite \"Prihvati poziv\" i upišite ovu šifru.");
        return new PorukaResponse("Poziv je poslan na " + email);
    }

    @Transactional
    public void ukloniClana(Long korisnikId) {
        KorisnikPrincipal ja = provjeriVlasnika();
        if (korisnikId.equals(ja.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ne možete ukloniti sami sebe");
        }
        Korisnik clan = korisnikRepository.findByIdAndTvrtkaId(korisnikId, ja.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Korisnik s id " + korisnikId + " nije član vaše tvrtke"));
        korisnikRepository.delete(clan);
    }

    private Tvrtka mojaTvrtka() {
        return tvrtkaRepository.findById(trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tvrtka ne postoji"));
    }

    private KorisnikPrincipal provjeriVlasnika() {
        KorisnikPrincipal ja = trenutniKorisnik.get();
        if (ja.uloga() != UlogaKorisnika.VLASNIK) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ovo može samo vlasnik tvrtke");
        }
        return ja;
    }
}
