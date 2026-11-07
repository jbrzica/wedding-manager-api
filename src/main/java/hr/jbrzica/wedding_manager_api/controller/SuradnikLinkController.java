package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.SuradnikLinkResponse;
import hr.jbrzica.wedding_manager_api.dto.SuradnikPregledResponse;
import hr.jbrzica.wedding_manager_api.service.SuradnikLinkService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuradnikLinkController {

    private static final MediaType ICAL = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final SuradnikLinkService suradnikLinkService;

    public SuradnikLinkController(SuradnikLinkService suradnikLinkService) {
        this.suradnikLinkService = suradnikLinkService;
    }

    @GetMapping("/api/suradnici/{id}/link")
    public SuradnikLinkResponse link(@PathVariable Long id) {
        return suradnikLinkService.link(id);
    }

    @PostMapping("/api/suradnici/{id}/link/novi")
    public SuradnikLinkResponse noviLink(@PathVariable Long id) {
        return suradnikLinkService.noviLink(id);
    }

    // Javno dostupno: pristup daje tajni token u adresi.
    @GetMapping("/api/javno/suradnik/{token}")
    public SuradnikPregledResponse pregled(@PathVariable String token) {
        return suradnikLinkService.pregled(token);
    }

    @GetMapping("/api/javno/suradnik/{token}/kalendar.ics")
    public ResponseEntity<String> kalendar(@PathVariable String token) {
        return ResponseEntity.ok().contentType(ICAL).body(suradnikLinkService.kalendar(token));
    }
}
