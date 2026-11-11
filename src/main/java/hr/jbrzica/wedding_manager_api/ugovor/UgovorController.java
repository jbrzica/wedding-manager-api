package hr.jbrzica.wedding_manager_api.ugovor;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class UgovorController {

    static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final UgovorService ugovorService;

    public UgovorController(UgovorService ugovorService) {
        this.ugovorService = ugovorService;
    }

    @GetMapping("/api/vjencanja/{id}/ugovor")
    public ResponseEntity<byte[]> ugovor(@PathVariable Long id) {
        return datoteka(ugovorService.ugovor(id));
    }

    @GetMapping("/api/tvrtka/ugovor")
    public PredlozakInfo info() {
        return ugovorService.info();
    }

    @GetMapping("/api/tvrtka/ugovor/predlozak")
    public ResponseEntity<byte[]> predlozak() {
        return datoteka(ugovorService.predlozak());
    }

    @PutMapping("/api/tvrtka/ugovor/predlozak")
    public PredlozakInfo spremi(@RequestParam("datoteka") MultipartFile datoteka) throws IOException {
        return ugovorService.spremi(datoteka.getOriginalFilename(), datoteka.getBytes());
    }

    @DeleteMapping("/api/tvrtka/ugovor/predlozak")
    public PredlozakInfo vratiZadani() {
        return ugovorService.vratiZadani();
    }

    private static ResponseEntity<byte[]> datoteka(Datoteka d) {
        // filename* s UTF-8, da č, ć, š, ž i đ u imenima mladenaca ostanu ispravni
        ContentDisposition preuzimanje = ContentDisposition.attachment().filename(d.naziv(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, preuzimanje.toString())
                .body(d.sadrzaj());
    }
}
