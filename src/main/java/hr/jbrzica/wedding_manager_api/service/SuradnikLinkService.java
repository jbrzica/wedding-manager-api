package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.SuradnikLinkResponse;
import hr.jbrzica.wedding_manager_api.dto.SuradnikPregledResponse;
import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Suradnik;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.repository.ClanEkipeRepository;
import hr.jbrzica.wedding_manager_api.repository.SuradnikRepository;
import hr.jbrzica.wedding_manager_api.security.TajniToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Osobni link suradnika: bez prijave vidi samo vjenčanja na kojima radi, bez ikakvih cijena.
 */
@Service
public class SuradnikLinkService {

    private static final ZoneId ZAGREB = ZoneId.of("Europe/Zagreb");

    private final SuradnikService suradnikService;
    private final SuradnikRepository suradnikRepository;
    private final ClanEkipeRepository clanEkipeRepository;
    private final KalendarService kalendarService;

    public SuradnikLinkService(SuradnikService suradnikService, SuradnikRepository suradnikRepository,
                               ClanEkipeRepository clanEkipeRepository, KalendarService kalendarService) {
        this.suradnikService = suradnikService;
        this.suradnikRepository = suradnikRepository;
        this.clanEkipeRepository = clanEkipeRepository;
        this.kalendarService = kalendarService;
    }

    @Transactional
    public SuradnikLinkResponse link(Long suradnikId) {
        Suradnik s = suradnikService.dohvatiEntitet(suradnikId);
        if (s.getPristupniToken() == null) {
            s.setPristupniToken(TajniToken.novi());
        }
        return SuradnikLinkResponse.za(s.getPristupniToken());
    }

    // Stari link odmah prestaje raditi, npr. kad suradnik više ne radi s tvrtkom.
    @Transactional
    public SuradnikLinkResponse noviLink(Long suradnikId) {
        Suradnik s = suradnikService.dohvatiEntitet(suradnikId);
        s.setPristupniToken(TajniToken.novi());
        return SuradnikLinkResponse.za(s.getPristupniToken());
    }

    @Transactional(readOnly = true)
    public SuradnikPregledResponse pregled(String token) {
        Suradnik s = poTokenu(token);
        LocalDate danas = LocalDate.now(ZAGREB);
        List<SuradnikPregledResponse.VjencanjeSuradnika> vjencanja = mojaMjesta(s).stream()
                .filter(c -> !c.getVjencanje().getDatum().isBefore(danas))
                .map(SuradnikPregledResponse.VjencanjeSuradnika::iz)
                .toList();
        return new SuradnikPregledResponse(s.getIme(), s.getTvrtka().getNaziv(),
                "/api/javno/suradnik/" + token + "/kalendar.ics", vjencanja);
    }

    @Transactional(readOnly = true)
    public String kalendar(String token) {
        Suradnik s = poTokenu(token);
        List<Vjencanje> vjencanja = mojaMjesta(s).stream().map(ClanEkipe::getVjencanje).distinct().toList();
        return kalendarService.izgradi("Vjenčanja – " + s.getTvrtka().getNaziv(), vjencanja);
    }

    // Provjera tvrtke je dodatna zaštita: suradnik se ionako može dodati samo na vjenčanja svoje tvrtke.
    private List<ClanEkipe> mojaMjesta(Suradnik s) {
        Long tvrtkaId = s.getTvrtka().getId();
        return clanEkipeRepository.findBySuradnikIdOrderByVjencanjeDatumAsc(s.getId()).stream()
                .filter(c -> c.getVjencanje().getTvrtka().getId().equals(tvrtkaId))
                .filter(c -> c.getVjencanje().getStatus() != StatusVjencanja.OTKAZANO)
                .toList();
    }

    private Suradnik poTokenu(String token) {
        return suradnikRepository.findByPristupniToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link nije ispravan ili više ne vrijedi"));
    }
}
