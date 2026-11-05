package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.PaketRequest;
import hr.jbrzica.wedding_manager_api.dto.PaketResponse;
import hr.jbrzica.wedding_manager_api.service.PaketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paketi")
public class PaketController {

    private final PaketService paketService;

    public PaketController(PaketService paketService) {
        this.paketService = paketService;
    }

    @GetMapping
    public List<PaketResponse> dohvatiSve() {
        return paketService.dohvatiSve();
    }

    @GetMapping("/{id}")
    public PaketResponse dohvati(@PathVariable Long id) {
        return paketService.dohvati(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaketResponse stvori(@Valid @RequestBody PaketRequest request) {
        return paketService.stvori(request);
    }

    @PutMapping("/{id}")
    public PaketResponse izmijeni(@PathVariable Long id, @Valid @RequestBody PaketRequest request) {
        return paketService.izmijeni(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        paketService.obrisi(id);
    }
}
