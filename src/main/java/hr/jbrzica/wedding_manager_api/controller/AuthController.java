package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registracija")
    @ResponseStatus(HttpStatus.CREATED)
    public PorukaResponse registracija(@Valid @RequestBody RegistracijaRequest request) {
        return authService.registracija(request);
    }

    @PostMapping("/potvrdi-email")
    public TokenResponse potvrdiEmail(@Valid @RequestBody PotvrdaEmailaRequest request) {
        return authService.potvrdiEmail(request);
    }

    @PostMapping("/ponovno-posalji-kod")
    public PorukaResponse ponovnoPosaljiKod(@Valid @RequestBody EmailRequest request) {
        return authService.ponovnoPosaljiKod(request);
    }

    @PostMapping("/prijava")
    public TokenResponse prijava(@Valid @RequestBody PrijavaRequest request) {
        return authService.prijava(request);
    }

    @PostMapping("/prihvati-poziv")
    public TokenResponse prihvatiPoziv(@Valid @RequestBody PrihvatiPozivRequest request) {
        return authService.prihvatiPoziv(request);
    }

    @PostMapping("/zaboravljena-lozinka")
    public PorukaResponse zaboravljenaLozinka(@Valid @RequestBody EmailRequest request) {
        return authService.zaboravljenaLozinka(request);
    }

    @PostMapping("/nova-lozinka")
    public TokenResponse novaLozinka(@Valid @RequestBody NovaLozinkaRequest request) {
        return authService.novaLozinka(request);
    }

    @GetMapping("/ja")
    public KorisnikResponse ja() {
        return authService.ja();
    }
}
