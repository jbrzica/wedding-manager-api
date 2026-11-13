package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.security.RefreshKolacic;
import hr.jbrzica.wedding_manager_api.security.RefreshTokenServis;
import hr.jbrzica.wedding_manager_api.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenServis refreshTokenServis;
    private final RefreshKolacic kolacic;

    public AuthController(AuthService authService, RefreshTokenServis refreshTokenServis, RefreshKolacic kolacic) {
        this.authService = authService;
        this.refreshTokenServis = refreshTokenServis;
        this.kolacic = kolacic;
    }

    @GetMapping("/postavke")
    public AuthPostavkeResponse postavke() {
        return authService.postavke();
    }

    @PostMapping("/registracija")
    @ResponseStatus(HttpStatus.CREATED)
    public PorukaResponse registracija(@Valid @RequestBody RegistracijaRequest request) {
        return authService.registracija(request);
    }

    @PostMapping("/potvrdi-email")
    public TokenResponse potvrdiEmail(@Valid @RequestBody PotvrdaEmailaRequest request, HttpServletResponse response) {
        return uzKolacic(authService.potvrdiEmail(request), response);
    }

    @PostMapping("/ponovno-posalji-kod")
    public PorukaResponse ponovnoPosaljiKod(@Valid @RequestBody EmailRequest request) {
        return authService.ponovnoPosaljiKod(request);
    }

    @PostMapping("/prijava")
    public TokenResponse prijava(@Valid @RequestBody PrijavaRequest request, HttpServletResponse response) {
        return uzKolacic(authService.prijava(request), response);
    }

    @PostMapping("/prihvati-poziv")
    public TokenResponse prihvatiPoziv(@Valid @RequestBody PrihvatiPozivRequest request, HttpServletResponse response) {
        return uzKolacic(authService.prihvatiPoziv(request), response);
    }

    @PostMapping("/zaboravljena-lozinka")
    public PorukaResponse zaboravljenaLozinka(@Valid @RequestBody EmailRequest request) {
        return authService.zaboravljenaLozinka(request);
    }

    // Nova lozinka preko maila: sve stare prijave se opozivaju, jer je možda netko znao staru lozinku
    @PostMapping("/nova-lozinka")
    public TokenResponse novaLozinka(@Valid @RequestBody NovaLozinkaRequest request, HttpServletResponse response) {
        TokenResponse odgovor = authService.novaLozinka(request);
        refreshTokenServis.opozoviSve(odgovor.korisnik().id());
        return uzKolacic(odgovor, response);
    }

    // Browser sam šalje kolačić; dobiva se novi kratki access token i novi kolačić (stari se troši)
    @PostMapping("/osvjezi")
    public TokenResponse osvjezi(@CookieValue(name = RefreshKolacic.IME, required = false) String token,
                                 HttpServletResponse response) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Niste prijavljeni");
        }
        try {
            RefreshTokenServis.Osvjezeno o = refreshTokenServis.osvjezi(token);
            kolacic.postavi(response, o.noviToken(), refreshTokenServis.trajanje());
            return o.odgovor();
        } catch (ResponseStatusException e) {
            kolacic.obrisi(response);
            throw e;
        }
    }

    @PostMapping("/odjava")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void odjava(@CookieValue(name = RefreshKolacic.IME, required = false) String token,
                       HttpServletResponse response) {
        if (token != null && !token.isBlank()) {
            refreshTokenServis.opozovi(token);
        }
        kolacic.obrisi(response);
    }

    // Ostali uređaji se odjavljuju, a ovaj ostaje prijavljen
    @PostMapping("/promjena-lozinke")
    public PorukaResponse promjenaLozinke(@Valid @RequestBody PromjenaLozinkeRequest request,
                                          @CookieValue(name = RefreshKolacic.IME, required = false) String token) {
        PorukaResponse odgovor = authService.promjenaLozinke(request);
        refreshTokenServis.opozoviOstale(authService.ja().id(), token);
        return odgovor;
    }

    @GetMapping("/ja")
    public KorisnikResponse ja() {
        return authService.ja();
    }

    private TokenResponse uzKolacic(TokenResponse odgovor, HttpServletResponse response) {
        kolacic.postavi(response, refreshTokenServis.izdaj(odgovor.korisnik().id()), refreshTokenServis.trajanje());
        return odgovor;
    }
}
