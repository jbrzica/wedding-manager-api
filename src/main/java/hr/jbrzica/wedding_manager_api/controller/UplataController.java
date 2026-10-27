package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.DugResponse;
import hr.jbrzica.wedding_manager_api.dto.UplataRequest;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.service.UplataService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vjencanja/{vjencanjeId}")
public class UplataController {

    private final UplataService uplataService;

    public UplataController(UplataService uplataService) {
        this.uplataService = uplataService;
    }

    @GetMapping("/uplate")
    public List<Uplata> dohvatiUplate(@PathVariable Long vjencanjeId) {
        return uplataService.dohvatiZaVjencanje(vjencanjeId);
    }

    @PostMapping("/uplate")
    @ResponseStatus(HttpStatus.CREATED)
    public Uplata stvori(@PathVariable Long vjencanjeId, @Valid @RequestBody UplataRequest request) {
        return uplataService.stvori(vjencanjeId, request);
    }

    @GetMapping("/dug")
    public DugResponse dug(@PathVariable Long vjencanjeId) {
        return uplataService.izracunajDug(vjencanjeId);
    }
}