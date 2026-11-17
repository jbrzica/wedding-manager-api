package hr.jbrzica.wedding_manager_api.termini;

import hr.jbrzica.wedding_manager_api.security.TrenutniKorisnik;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Naslovna slika javne stranice. Preglednik je prije slanja smanji, pa je obično 100-300 KB;
 * zato je dovoljno čuvati je u bazi, bez posebnog spremišta datoteka.
 */
@Service
public class SlikaTerminaService {

    static final int NAJVISE_BAJTOVA = 3 * 1024 * 1024;

    public record Slika(String tip, byte[] sadrzaj) {
    }

    private final DogadjajTerminaRepository dogadjajRepository;
    private final SlikaTerminaRepository slikaRepository;
    private final TrenutniKorisnik trenutniKorisnik;

    public SlikaTerminaService(DogadjajTerminaRepository dogadjajRepository, SlikaTerminaRepository slikaRepository,
                               TrenutniKorisnik trenutniKorisnik) {
        this.dogadjajRepository = dogadjajRepository;
        this.slikaRepository = slikaRepository;
        this.trenutniKorisnik = trenutniKorisnik;
    }

    @Transactional
    public int spremi(Long dogadjajId, byte[] sadrzaj) {
        DogadjajTermina d = dohvati(dogadjajId);
        if (sadrzaj.length == 0 || sadrzaj.length > NAJVISE_BAJTOVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slika smije imati najviše 3 MB.");
        }
        // Tip se određuje po sadržaju, ne po nazivu ili zaglavlju koje šalje klijent
        String tip = tip(sadrzaj);
        if (tip == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slika mora biti JPG, PNG ili WebP.");
        }
        SlikaTermina s = slikaRepository.findById(d.getId()).orElseGet(SlikaTermina::new);
        s.setDogadjajId(d.getId());
        s.setTip(tip);
        s.setSadrzaj(sadrzaj);
        s.setAzurirano(Instant.now());
        slikaRepository.save(s);
        d.setSlikaVerzija(d.getSlikaVerzija() + 1);
        return d.getSlikaVerzija();
    }

    @Transactional
    public void obrisi(Long dogadjajId) {
        DogadjajTermina d = dohvati(dogadjajId);
        slikaRepository.deleteById(d.getId());
        d.setSlikaVerzija(0);
    }

    @Transactional(readOnly = true)
    public Slika javna(String kod) {
        DogadjajTermina d = dogadjajRepository.findByJavniKod(kod)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return slikaRepository.findById(d.getId())
                .map(s -> new Slika(s.getTip(), s.getSadrzaj()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private DogadjajTermina dohvati(Long id) {
        return dogadjajRepository.findByIdAndTvrtkaId(id, trenutniKorisnik.tvrtkaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografiranje ne postoji."));
    }

    static String tip(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
