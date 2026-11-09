package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.KosStavka;
import hr.jbrzica.wedding_manager_api.service.KosService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kos")
public class KosController {

    private final KosService kosService;

    public KosController(KosService kosService) {
        this.kosService = kosService;
    }

    @GetMapping
    public List<KosStavka> popis() {
        return kosService.popis();
    }

    @PostMapping("/{id}/vrati")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vrati(@PathVariable Long id) {
        kosService.vrati(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisiTrajno(@PathVariable Long id) {
        kosService.obrisiTrajno(id);
    }
}
