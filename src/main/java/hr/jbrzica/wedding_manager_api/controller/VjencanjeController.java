package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.VjencanjeRequest;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.service.VjencanjeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vjencanja")
public class VjencanjeController {

    private final VjencanjeService vjencanjeService;

    public VjencanjeController(VjencanjeService vjencanjeService) {
        this.vjencanjeService = vjencanjeService;
    }

    @GetMapping
    public List<Vjencanje> dohvatiSve(@RequestParam(required = false) StatusVjencanja status) {
        return vjencanjeService.dohvatiSve(status);
    }

    @GetMapping("/{id}")
    public Vjencanje dohvatiPoId(@PathVariable Long id) {
        return vjencanjeService.dohvatiPoId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vjencanje stvori(@RequestBody VjencanjeRequest request) {
        return vjencanjeService.stvori(request);
    }

    @PatchMapping("/{id}/status")
    public Vjencanje promijeniStatus(@PathVariable Long id, @RequestParam StatusVjencanja status) {
        return vjencanjeService.promijeniStatus(id, status);
    }

    @PostMapping("/{id}/dodaci/{dodatakId}")
    public Vjencanje dodajDodatak(@PathVariable Long id, @PathVariable Long dodatakId) {
        return vjencanjeService.dodajDodatak(id, dodatakId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        vjencanjeService.obrisi(id);
    }
}