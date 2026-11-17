package hr.jbrzica.wedding_manager_api.termini;

import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DogadjajDetalji;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DogadjajRequest;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.DogadjajSazetak;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.JavnaRezervacijaRequest;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.JavniDogadjaj;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.PotvrdaRezervacije;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.RezervacijaResponse;
import hr.jbrzica.wedding_manager_api.termini.TerminiDto.RucnaRezervacijaRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
public class TerminiController {

    private final TerminiService terminiService;
    private final JavniTerminiService javniService;
    private final SlikaTerminaService slikaService;

    public TerminiController(TerminiService terminiService, JavniTerminiService javniService,
                             SlikaTerminaService slikaService) {
        this.terminiService = terminiService;
        this.javniService = javniService;
        this.slikaService = slikaService;
    }

    // ---------- fotograf (prijava obavezna) ----------

    @GetMapping("/api/termini")
    public List<DogadjajSazetak> popis() {
        return terminiService.popis();
    }

    @PostMapping("/api/termini")
    @ResponseStatus(HttpStatus.CREATED)
    public DogadjajDetalji stvori(@Valid @RequestBody DogadjajRequest request) {
        return terminiService.stvori(request);
    }

    @GetMapping("/api/termini/{id}")
    public DogadjajDetalji detalji(@PathVariable Long id) {
        return terminiService.detalji(id);
    }

    @PutMapping("/api/termini/{id}")
    public DogadjajDetalji izmijeni(@PathVariable Long id, @Valid @RequestBody DogadjajRequest request) {
        return terminiService.izmijeni(id, request);
    }

    @DeleteMapping("/api/termini/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisi(@PathVariable Long id) {
        terminiService.obrisi(id);
    }

    @PostMapping("/api/termini/{id}/novi-link")
    public DogadjajDetalji noviLink(@PathVariable Long id) {
        return terminiService.noviLink(id);
    }

    @PutMapping("/api/termini/{id}/slika")
    public Map<String, Integer> spremiSliku(@PathVariable Long id, @RequestParam("slika") MultipartFile slika) throws IOException {
        return Map.of("slikaVerzija", slikaService.spremi(id, slika.getBytes()));
    }

    @DeleteMapping("/api/termini/{id}/slika")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void obrisiSliku(@PathVariable Long id) {
        slikaService.obrisi(id);
    }

    @PostMapping("/api/termini/{id}/rezervacije")
    @ResponseStatus(HttpStatus.CREATED)
    public RezervacijaResponse rucnaRezervacija(@PathVariable Long id, @Valid @RequestBody RucnaRezervacijaRequest request) {
        return terminiService.rucnaRezervacija(id, request);
    }

    @GetMapping("/api/termini/nove")
    public TerminiDto.NoveRezervacije nove() {
        return terminiService.nove();
    }

    @PostMapping("/api/termini/nove/procitano")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void procitano() {
        terminiService.procitano();
    }

    @PostMapping("/api/termini/rezervacije/{id}/otkazi")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void otkazi(@PathVariable Long id) {
        terminiService.otkazi(id);
    }

    // ---------- javno: pristup daje tajni kod u linku ----------

    @GetMapping("/api/javno/termini/{kod}")
    public JavniDogadjaj javniDogadjaj(@PathVariable String kod) {
        return javniService.dogadjaj(kod);
    }

    // Adresa sadrži verziju (?v=), pa preglednik smije sliku čuvati dugo
    @GetMapping("/api/javno/termini/{kod}/slika")
    public ResponseEntity<byte[]> javnaSlika(@PathVariable String kod) {
        SlikaTerminaService.Slika s = slikaService.javna(kod);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(s.tip()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(s.sadrzaj());
    }

    @PostMapping("/api/javno/termini/{kod}/rezervacije")
    @ResponseStatus(HttpStatus.CREATED)
    public PotvrdaRezervacije rezerviraj(@PathVariable String kod, @Valid @RequestBody JavnaRezervacijaRequest request) {
        return javniService.rezerviraj(kod, request);
    }

    @GetMapping("/api/javno/otkazivanje/{otkazniKod}")
    public PotvrdaRezervacije rezervacija(@PathVariable String otkazniKod) {
        return javniService.poOtkaznomKodu(otkazniKod);
    }

    @PostMapping("/api/javno/otkazivanje/{otkazniKod}")
    public PotvrdaRezervacije otkaziJavno(@PathVariable String otkazniKod) {
        return javniService.otkazi(otkazniKod);
    }
}
