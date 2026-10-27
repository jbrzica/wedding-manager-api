package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.service.DodatakService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dodaci")
public class DodatakController {

    private final DodatakService dodatakService;

    public DodatakController(DodatakService dodatakService) {
        this.dodatakService = dodatakService;
    }

    @GetMapping
    public List<Dodatak> dohvatiSve() {
        return dodatakService.dohvatiSve();
    }

    @GetMapping("/{id}")
    public Dodatak dohvatiPoId(@PathVariable Long id) {
        return dodatakService.dohvatiPoId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dodatak spremi(@Valid @RequestBody Dodatak dodatak) {
        return dodatakService.spremi(dodatak);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        dodatakService.obrisi(id);
    }
}