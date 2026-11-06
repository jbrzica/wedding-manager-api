package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.KalendarLinkResponse;
import hr.jbrzica.wedding_manager_api.service.KalendarService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KalendarController {

    private static final MediaType ICAL = MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final KalendarService kalendarService;

    public KalendarController(KalendarService kalendarService) {
        this.kalendarService = kalendarService;
    }

    @GetMapping("/api/kalendar/link")
    public KalendarLinkResponse link() {
        return kalendarService.link();
    }

    @PostMapping("/api/kalendar/link/novi")
    public KalendarLinkResponse noviLink() {
        return kalendarService.noviLink();
    }

    // Javno dostupno: pristup daje sam tajni token u adresi.
    @GetMapping("/api/ical/{token}.ics")
    public ResponseEntity<String> ical(@PathVariable String token) {
        return ResponseEntity.ok().contentType(ICAL).body(kalendarService.ical(token));
    }
}
