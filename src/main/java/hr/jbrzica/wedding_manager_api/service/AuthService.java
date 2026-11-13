package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.AuthPostavkeResponse;
import org.springframework.beans.factory.annotation.Value;
import java.util.Optional;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.entity.*;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import hr.jbrzica.wedding_manager_api.repository.TvrtkaRepository;
import hr.jbrzica.wedding_manager_api.security.JwtServis;
import hr.jbrzica.wedding_manager_api.security.PrijavaLimiter;
import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final KorisnikRepository korisnikRepository;
    private final TvrtkaRepository tvrtkaRepository;
    private final PasswordEncoder passwordEncoder;
    private final KodServis kodServis;
    private final JwtServis jwtServis;
    private final TrenutniKorisnik trenutniKorisnik;
    private final PrijavaLimiter prijavaLimiter;
    private final boolean registracijaOtvorena;
    // Hash nepostojeće lozinke: prijava s nepoznatim emailom troši jednako vremena kao s postojećim
    private final String laziHash;

    public AuthService(KorisnikRepository korisnikRepository, TvrtkaRepository tvrtkaRepository,
                       PasswordEncoder passwordEncoder, KodServis kodServis, JwtServis jwtServis,
                       TrenutniKorisnik trenutniKorisnik, PrijavaLimiter prijavaLimiter,
                       @Value("${app.registracija.otvorena:false}") boolean registracijaOtvorena) {
        this.korisnikRepository = korisnikRepository;
        this.tvrtkaRepository = tvrtkaRepository;
        this.passwordEncoder = passwordEncoder;
        this.kodServis = kodServis;
        this.jwtServis = jwtServis;
        this.trenutniKorisnik = trenutniKorisnik;
        this.prijavaLimiter = prijavaLimiter;
        this.registracijaOtvorena = registracijaOtvorena;
        this.laziHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    public AuthPostavkeResponse postavke() {
        return new AuthPostavkeResponse(registracijaOtvorena);
    }

    // Nova tvrtka i njezin vlasnik; račun je aktivan tek nakon potvrde emaila.
    @Transactional
    public PorukaResponse registracija(RegistracijaRequest request) {
        // Zatvorena registracija: nitko ne može napraviti račun s tuđim emailom ni slati mailove preko aplikacije
        if (!registracijaOtvorena) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Registracija novih tvrtki je zatvorena. Za pristup se javi vlasniku aplikacije.");
        }
        String email = normaliziraj(request.email());
        if (korisnikRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Račun s tim emailom već postoji");
        }

        Tvrtka tvrtka = new Tvrtka();
        tvrtka.setNaziv(request.nazivTvrtke());
        tvrtkaRepository.save(tvrtka);

        Korisnik korisnik = new Korisnik();
        korisnik.setIme(request.ime());
        korisnik.setEmail(email);
        korisnik.setLozinkaHash(passwordEncoder.encode(request.lozinka()));
        korisnik.setUloga(UlogaKorisnika.VLASNIK);
        korisnik.setEmailPotvrden(false);
        korisnik.setTvrtka(tvrtka);
        korisnikRepository.save(korisnik);

        posaljiKodZaPotvrdu(email);
        return new PorukaResponse("Račun je napravljen. Na " + email + " smo poslali šifru za potvrdu.");
    }

    @Transactional
    public TokenResponse potvrdiEmail(PotvrdaEmailaRequest request) {
        String email = normaliziraj(request.email());
        Korisnik korisnik = korisnikRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Šifra nije ispravna ili je istekla. Zatražite novu šifru."));

        kodServis.provjeri(email, SvrhaKoda.POTVRDA_EMAILA, request.kod());
        korisnik.setEmailPotvrden(true);
        return token(korisnik);
    }

    // Uvijek odgovori isto, da se ne može saznati postoji li račun s tim emailom.
    @Transactional
    public PorukaResponse ponovnoPosaljiKod(EmailRequest request) {
        String email = normaliziraj(request.email());
        korisnikRepository.findByEmail(email)
                .filter(k -> !k.isEmailPotvrden())
                .ifPresent(k -> posaljiKodZaPotvrdu(email));
        return new PorukaResponse("Ako račun postoji i nije potvrđen, poslali smo novu šifru.");
    }

    @Transactional(readOnly = true)
    public TokenResponse prijava(PrijavaRequest request) {
        String email = normaliziraj(request.email());
        prijavaLimiter.provjeri(email);
        Optional<Korisnik> nadjen = korisnikRepository.findByEmail(email);
        // BCrypt se računa i kad račun ne postoji, da se po brzini odgovora ne vidi koji emailovi imaju račun
        boolean lozinkaOk = passwordEncoder.matches(request.lozinka(),
                nadjen.map(Korisnik::getLozinkaHash).orElse(laziHash));
        Korisnik korisnik = nadjen.filter(k -> lozinkaOk)
                .orElseThrow(() -> {
                    prijavaLimiter.neuspjeh(email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pogrešan email ili lozinka");
                });

        if (!korisnik.isEmailPotvrden()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Email nije potvrđen. Upišite šifru koju ste dobili mailom.");
        }
        prijavaLimiter.uspjeh(email);
        return token(korisnik);
    }

    // Pozvani korisnik postaje član tvrtke; šifra iz maila ujedno potvrđuje njegov email.
    @Transactional
    public TokenResponse prihvatiPoziv(PrihvatiPozivRequest request) {
        String email = normaliziraj(request.email());
        // Prvo šifra, pa tek onda postoji li račun: bez ispravne šifre odgovor ne smije otkriti tko ima račun
        EmailKod poziv = kodServis.provjeri(email, SvrhaKoda.POZIV, request.kod());
        if (korisnikRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Račun s tim emailom već postoji");
        }
        Long tvrtkaId = poziv.getTvrtka().getId();

        Korisnik korisnik = new Korisnik();
        korisnik.setIme(request.ime());
        korisnik.setEmail(email);
        korisnik.setLozinkaHash(passwordEncoder.encode(request.lozinka()));
        korisnik.setUloga(UlogaKorisnika.CLAN);
        korisnik.setEmailPotvrden(true);
        korisnik.setTvrtka(tvrtkaRepository.getReferenceById(tvrtkaId));
        korisnikRepository.save(korisnik);

        return token(korisnik);
    }

    // Isto kao kod ponovnog slanja: odgovor ne otkriva postoji li račun.
    @Transactional
    public PorukaResponse zaboravljenaLozinka(EmailRequest request) {
        String email = normaliziraj(request.email());
        if (korisnikRepository.existsByEmail(email)) {
            kodServis.posaljiKod(email, SvrhaKoda.RESET_LOZINKE, null,
                    "Nova lozinka – Wedding Manager",
                    "Zatražili ste novu lozinku. Upišite ovu šifru u aplikaciju i odaberite novu lozinku.\n"
                            + "Ako niste vi zatražili, samo zanemarite ovaj mail.");
        }
        return new PorukaResponse("Ako račun s tim emailom postoji, poslali smo šifru za novu lozinku.");
    }

    @Transactional
    public TokenResponse novaLozinka(NovaLozinkaRequest request) {
        String email = normaliziraj(request.email());
        Korisnik korisnik = korisnikRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Šifra nije ispravna ili je istekla. Zatražite novu šifru."));

        kodServis.provjeri(email, SvrhaKoda.RESET_LOZINKE, request.kod());
        korisnik.setLozinkaHash(passwordEncoder.encode(request.lozinka()));
        // Šifra je stigla na taj email, pa je i email time potvrđen
        korisnik.setEmailPotvrden(true);
        return token(korisnik);
    }

    // Prijavljeni korisnik mijenja lozinku; stara se traži da ukradeni token nije dovoljan.
    @Transactional
    public PorukaResponse promjenaLozinke(PromjenaLozinkeRequest request) {
        Korisnik korisnik = korisnikRepository.findById(trenutniKorisnik.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niste prijavljeni"));
        if (!passwordEncoder.matches(request.staraLozinka(), korisnik.getLozinkaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trenutna lozinka nije ispravna");
        }
        korisnik.setLozinkaHash(passwordEncoder.encode(request.novaLozinka()));
        return new PorukaResponse("Lozinka je promijenjena.");
    }

    @Transactional(readOnly = true)
    public KorisnikResponse ja() {
        Korisnik korisnik = korisnikRepository.findById(trenutniKorisnik.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niste prijavljeni"));
        return KorisnikResponse.iz(korisnik);
    }

    public static String normaliziraj(String email) {
        return email.trim().toLowerCase();
    }

    private void posaljiKodZaPotvrdu(String email) {
        kodServis.posaljiKod(email, SvrhaKoda.POTVRDA_EMAILA, null,
                "Potvrda emaila – Wedding Manager",
                "Dobrodošli u Wedding Manager! Upišite ovu šifru u aplikaciju da potvrdite svoj email.");
    }

    private TokenResponse token(Korisnik korisnik) {
        return new TokenResponse(jwtServis.napravi(korisnik), KorisnikResponse.iz(korisnik));
    }
}
