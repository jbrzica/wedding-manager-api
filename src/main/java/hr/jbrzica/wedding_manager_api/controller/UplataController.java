package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.UplataRequest;
import hr.jbrzica.wedding_manager_api.dto.UplataResponse;
import hr.jbrzica.wedding_manager_api.service.UplataService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vjencanja/{vjencanjeId}/uplate")
public class UplataController {

    private final UplataService uplataService;

    public UplataController(UplataService uplataService) {
        this.uplataService = uplataService;
    }

    @GetMapping
    public List<UplataResponse> dohvatiUplate(@PathVariable Long vjencanjeId) {
        return uplataService.dohvatiZaVjencanje(vjencanjeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UplataResponse stvori(@PathVariable Long vjencanjeId, @Valid @RequestBody UplataRequest request) {
        return uplataService.stvori(vjencanjeId, request);
    }

    @DeleteMapping("/{uplataId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long vjencanjeId, @PathVariable Long uplataId) {
        uplataService.obrisi(vjencanjeId, uplataId);
    }
}
