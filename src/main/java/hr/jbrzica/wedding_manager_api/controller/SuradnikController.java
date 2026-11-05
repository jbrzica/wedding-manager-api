package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.SuradnikRequest;
import hr.jbrzica.wedding_manager_api.dto.SuradnikResponse;
import hr.jbrzica.wedding_manager_api.service.SuradnikService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suradnici")
public class SuradnikController {

    private final SuradnikService suradnikService;

    public SuradnikController(SuradnikService suradnikService) {
        this.suradnikService = suradnikService;
    }

    @GetMapping
    public List<SuradnikResponse> dohvatiSve() {
        return suradnikService.dohvatiSve();
    }

    @GetMapping("/{id}")
    public SuradnikResponse dohvati(@PathVariable Long id) {
        return suradnikService.dohvati(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuradnikResponse stvori(@Valid @RequestBody SuradnikRequest request) {
        return suradnikService.stvori(request);
    }

    @PutMapping("/{id}")
    public SuradnikResponse izmijeni(@PathVariable Long id, @Valid @RequestBody SuradnikRequest request) {
        return suradnikService.izmijeni(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        suradnikService.obrisi(id);
    }
}
