package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.ZadatakRequest;
import hr.jbrzica.wedding_manager_api.dto.ZadatakResponse;
import hr.jbrzica.wedding_manager_api.service.ZadatakService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vjencanja/{vjencanjeId}/zadaci")
public class ZadatakController {

    private final ZadatakService zadatakService;

    public ZadatakController(ZadatakService zadatakService) {
        this.zadatakService = zadatakService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZadatakResponse dodaj(@PathVariable Long vjencanjeId, @Valid @RequestBody ZadatakRequest request) {
        return zadatakService.dodaj(vjencanjeId, request);
    }

    @PatchMapping("/{zadatakId}")
    public ZadatakResponse oznaci(@PathVariable Long vjencanjeId, @PathVariable Long zadatakId,
                                  @RequestParam boolean gotovo) {
        return zadatakService.oznaci(vjencanjeId, zadatakId, gotovo);
    }

    @DeleteMapping("/{zadatakId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long vjencanjeId, @PathVariable Long zadatakId) {
        zadatakService.obrisi(vjencanjeId, zadatakId);
    }
}
