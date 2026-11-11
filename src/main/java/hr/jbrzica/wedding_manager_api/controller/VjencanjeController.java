package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.*;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.service.VjencanjeService;
import jakarta.validation.Valid;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vjencanja")
public class VjencanjeController {

    private final VjencanjeService vjencanjeService;

    public VjencanjeController(VjencanjeService vjencanjeService) {
        this.vjencanjeService = vjencanjeService;
    }

    @GetMapping
    public PagedModel<VjencanjeSazetak> popis(
            @RequestParam(defaultValue = "NADOLAZECA") Razdoblje razdoblje,
            @RequestParam(required = false) Integer godina,
            @RequestParam(name = "status", required = false) List<StatusVjencanja> statusi,
            @RequestParam(required = false) String pretraga,
            @RequestParam(defaultValue = "false") boolean nenaplaceno,
            @RequestParam(defaultValue = "false") boolean kasniIsporuka,
            @RequestParam(defaultValue = "false") boolean bezKapare,
            @RequestParam(defaultValue = "0") int stranica,
            @RequestParam(defaultValue = "50") int velicina) {
        return new PagedModel<>(vjencanjeService.popis(razdoblje, godina, statusi, pretraga, nenaplaceno, kasniIsporuka, bezKapare,
                stranica, velicina));
    }

    @GetMapping("/kalendar")
    public List<VjencanjeSazetak> kalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate od,
            @RequestParam("do") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate doDatuma) {
        return vjencanjeService.kalendar(od, doDatuma);
    }

    @GetMapping("/{id}/povijest")
    public List<PromjenaResponse> povijest(@PathVariable Long id) {
        return vjencanjeService.povijest(id);
    }

    @GetMapping("/prognoze")
    public Map<Long, PrognozaResponse> prognoze() {
        return vjencanjeService.prognoze();
    }

    @GetMapping("/dostupnost")
    public DostupnostResponse dostupnost(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum) {
        return vjencanjeService.dostupnost(datum);
    }

    @GetMapping("/upozorenja")
    public UpozorenjaResponse upozorenja(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum,
            @RequestParam(required = false) List<Long> suradnikIds,
            @RequestParam(required = false) Long izuzmiId) {
        return vjencanjeService.provjeriPreklapanja(datum, suradnikIds, izuzmiId);
    }

    @GetMapping("/pregled")
    public PregledResponse pregled() {
        return vjencanjeService.pregled();
    }

    @GetMapping("/{id}")
    public VjencanjeDetalji detalji(@PathVariable Long id) {
        return vjencanjeService.detalji(id);
    }

    @GetMapping("/{id}/financije")
    public FinancijeResponse financije(@PathVariable Long id) {
        return vjencanjeService.financije(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VjencanjeDetalji stvori(@Valid @RequestBody VjencanjeRequest request) {
        return vjencanjeService.stvori(request);
    }

    @PostMapping("/upit")
    @ResponseStatus(HttpStatus.CREATED)
    public VjencanjeDetalji brziUpit(@Valid @RequestBody BrziUpitRequest request) {
        return vjencanjeService.stvoriUpit(request);
    }

    @PutMapping("/{id}")
    public VjencanjeDetalji izmijeni(@PathVariable Long id, @Valid @RequestBody VjencanjeRequest request) {
        return vjencanjeService.izmijeni(id, request);
    }

    @PatchMapping("/{id}/status")
    public VjencanjeDetalji promijeniStatus(@PathVariable Long id, @RequestParam StatusVjencanja status) {
        return vjencanjeService.promijeniStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        vjencanjeService.obrisi(id);
    }
}
