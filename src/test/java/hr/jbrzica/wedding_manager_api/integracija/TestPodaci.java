package hr.jbrzica.wedding_manager_api.integracija;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.entity.UlogaKorisnika;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Korisnici se spremaju izravno u bazu, da test ne ovisi o slanju maila s kodom za potvrdu.
@Component
class TestPodaci {

    private final TvrtkaRepository tvrtkaRepository;
    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;

    TestPodaci(TvrtkaRepository tvrtkaRepository, KorisnikRepository korisnikRepository,
               PasswordEncoder passwordEncoder) {
        this.tvrtkaRepository = tvrtkaRepository;
        this.korisnikRepository = korisnikRepository;
        this.passwordEncoder = passwordEncoder;
    }

    Korisnik noviVlasnik(String nazivTvrtke, String lozinka) {
        Tvrtka tvrtka = new Tvrtka();
        tvrtka.setNaziv(nazivTvrtke);
        tvrtkaRepository.save(tvrtka);

        Korisnik korisnik = new Korisnik();
        korisnik.setIme("Vlasnik");
        // Svaki test dobije novi email, pa testovi ne smetaju jedan drugome
        korisnik.setEmail(UUID.randomUUID() + "@test.hr");
        korisnik.setLozinkaHash(passwordEncoder.encode(lozinka));
        korisnik.setUloga(UlogaKorisnika.VLASNIK);
        korisnik.setEmailPotvrden(true);
        korisnik.setTvrtka(tvrtka);
        return korisnikRepository.save(korisnik);
    }
}
