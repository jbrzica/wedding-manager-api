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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TerminiController {

    private final TerminiService terminiService;
    private final JavniTerminiService javniService;

    public TerminiController(TerminiService terminiService, JavniTerminiService javniService) {
        this.terminiService = terminiService;
        this.javniService = javniService;
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

    @PostMapping("/api/termini/{id}/rezervacije")
    @ResponseStatus(HttpStatus.CREATED)
    public RezervacijaResponse rucnaRezervacija(@PathVariable Long id, @Valid @RequestBody RucnaRezervacijaRequest request) {
        return terminiService.rucnaRezervacija(id, request);
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
