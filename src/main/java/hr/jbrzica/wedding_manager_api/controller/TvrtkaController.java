package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.EmailRequest;
import hr.jbrzica.wedding_manager_api.dto.PorukaResponse;
import hr.jbrzica.wedding_manager_api.dto.PrviKoraciResponse;
import hr.jbrzica.wedding_manager_api.dto.TvrtkaRequest;
import hr.jbrzica.wedding_manager_api.dto.TvrtkaResponse;
import hr.jbrzica.wedding_manager_api.dto.StavkaRasporedaDto;
import hr.jbrzica.wedding_manager_api.service.PredlozakService;
import hr.jbrzica.wedding_manager_api.service.TvrtkaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tvrtka")
public class TvrtkaController {

    private final TvrtkaService tvrtkaService;
    private final PredlozakService predlozakService;

    public TvrtkaController(TvrtkaService tvrtkaService, PredlozakService predlozakService) {
        this.tvrtkaService = tvrtkaService;
        this.predlozakService = predlozakService;
    }

    @GetMapping("/predlozak-rasporeda")
    public List<StavkaRasporedaDto> predlozak() {
        return predlozakService.dohvati();
    }

    @PutMapping("/predlozak-rasporeda")
    public List<StavkaRasporedaDto> spremiPredlozak(@RequestBody List<@Valid StavkaRasporedaDto> stavke) {
        return predlozakService.spremi(stavke);
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
