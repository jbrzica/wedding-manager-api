package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.PromjenaResponse;
import hr.jbrzica.wedding_manager_api.service.PovijestServis;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/povijest")
public class PovijestController {

    private final PovijestServis povijestServis;

    public PovijestController(PovijestServis povijestServis) {
        this.povijestServis = povijestServis;
    }

    // Zadnjih 200 promjena u tvrtki, po želji samo jedne osobe
    @GetMapping
    public List<PromjenaResponse> popis(@RequestParam(required = false) Long korisnikId) {
        return povijestServis.zaTvrtku(korisnikId);
    }
}
