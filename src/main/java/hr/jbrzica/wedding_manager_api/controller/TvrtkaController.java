package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.EmailRequest;
import hr.jbrzica.wedding_manager_api.dto.PorukaResponse;
import hr.jbrzica.wedding_manager_api.dto.PrviKoraciResponse;
import hr.jbrzica.wedding_manager_api.dto.TvrtkaRequest;
import hr.jbrzica.wedding_manager_api.dto.TvrtkaResponse;
import hr.jbrzica.wedding_manager_api.service.TvrtkaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tvrtka")
public class TvrtkaController {

    private final TvrtkaService tvrtkaService;

    public TvrtkaController(TvrtkaService tvrtkaService) {
        this.tvrtkaService = tvrtkaService;
    }

    @GetMapping
    public TvrtkaResponse moja() {
        return tvrtkaService.moja();
    }

    @GetMapping("/prvi-koraci")
    public PrviKoraciResponse prviKoraci() {
        return tvrtkaService.prviKoraci();
    }

    @PutMapping
    public TvrtkaResponse preimenuj(@Valid @RequestBody TvrtkaRequest request) {
        return tvrtkaService.preimenuj(request);
    }

    @PostMapping("/pozivi")
    public PorukaResponse pozovi(@Valid @RequestBody EmailRequest request) {
        return tvrtkaService.pozovi(request);
    }

    @DeleteMapping("/clanovi/{korisnikId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ukloniClana(@PathVariable Long korisnikId) {
        tvrtkaService.ukloniClana(korisnikId);
    }
}
