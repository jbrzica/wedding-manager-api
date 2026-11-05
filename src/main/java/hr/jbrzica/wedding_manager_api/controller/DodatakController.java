package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.DodatakRequest;
import hr.jbrzica.wedding_manager_api.dto.DodatakResponse;
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
    public List<DodatakResponse> dohvatiSve() {
        return dodatakService.dohvatiSve();
    }

    @GetMapping("/{id}")
    public DodatakResponse dohvati(@PathVariable Long id) {
        return dodatakService.dohvati(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DodatakResponse stvori(@Valid @RequestBody DodatakRequest request) {
        return dodatakService.stvori(request);
    }

    @PutMapping("/{id}")
    public DodatakResponse izmijeni(@PathVariable Long id, @Valid @RequestBody DodatakRequest request) {
        return dodatakService.izmijeni(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        dodatakService.obrisi(id);
    }
}
