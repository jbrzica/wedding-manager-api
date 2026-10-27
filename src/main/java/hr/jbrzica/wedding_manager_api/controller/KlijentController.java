package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.entity.Klijent;
import hr.jbrzica.wedding_manager_api.service.KlijentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/klijenti")
public class KlijentController {

    private final KlijentService klijentService;

    public KlijentController(KlijentService klijentService) {
        this.klijentService = klijentService;
    }

    @GetMapping
    public List<Klijent> dohvatiSve() {
        return klijentService.dohvatiSve();
    }

    @GetMapping("/{id}")
    public Klijent dohvatiPoId(@PathVariable Long id) {
        return klijentService.dohvatiPoId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Klijent spremi(@Valid @RequestBody Klijent klijent) {
        return klijentService.spremi(klijent);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        klijentService.obrisi(id);
    }
}