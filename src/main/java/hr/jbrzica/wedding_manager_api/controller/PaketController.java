package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.entity.Paket;
import hr.jbrzica.wedding_manager_api.service.PaketService;
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
    public List<Paket> dohvatiSve() {
        return paketService.dohvatiSve();
    }

    @GetMapping("/{id}")
    public Paket dohvatiPoId(@PathVariable Long id) {
        return paketService.dohvatiPoId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Paket spremi(@RequestBody Paket paket) {
        return paketService.spremi(paket);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        paketService.obrisi(id);
    }
}