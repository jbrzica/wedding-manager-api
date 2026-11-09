package hr.jbrzica.wedding_manager_api.security;

import hr.jbrzica.wedding_manager_api.dto.KorisnikResponse;
import hr.jbrzica.wedding_manager_api.dto.TokenResponse;
import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import hr.jbrzica.wedding_manager_api.entity.RefreshToken;
import hr.jbrzica.wedding_manager_api.repository.KorisnikRepository;
import hr.jbrzica.wedding_manager_api.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Refresh tokeni s rotacijom: svako osvježavanje troši stari token i daje novi.
 * Ako netko pokuša iskoristiti već potrošen token, to znači da ga je ukrao,
 * pa se opoziva cijela prijava (sva obitelj tokena) i oba moraju se ponovno prijaviti.
 */
@Service
public class RefreshTokenServis {

    // Odgovor se slaže unutar transakcije, jer treba podatke tvrtke koji se učitavaju lijeno
    public record Osvjezeno(TokenResponse odgovor, String noviToken) {
    }

    private final RefreshTokenRepository repository;
    private final KorisnikRepository korisnikRepository;
    private final JwtServis jwtServis;
    private final Duration trajanje;

    public RefreshTokenServis(RefreshTokenRepository repository, KorisnikRepository korisnikRepository,
                              JwtServis jwtServis, @Value("${app.refresh.trajanje-dana}") long trajanjeDana) {
        this.repository = repository;
        this.korisnikRepository = korisnikRepository;
        this.jwtServis = jwtServis;
        this.trajanje = Duration.ofDays(trajanjeDana);
    }

    public Duration trajanje() {
        return trajanje;
    }

    // Nova prijava: nova obitelj tokena
    @Transactional
    public String izdaj(Long korisnikId) {
        return izdaj(korisnikRepository.getReferenceById(korisnikId), TajniToken.novi());
    }

    // noRollbackFor: opoziv ukradene obitelji mora ostati spremljen iako metoda baci 401
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Osvjezeno osvjezi(String token) {
        RefreshToken stari = repository.findByTokenHash(hash(token))
                .orElseThrow(RefreshTokenServis::neispravan);
        if (stari.getIskoristen() != null) {
            repository.obrisiObitelj(stari.getObitelj());
            throw neispravan();
        }
        if (stari.getIstjece().isBefore(Instant.now()) || !stari.getKorisnik().isEmailPotvrden()) {
            throw neispravan();
        }
        stari.setIskoristen(Instant.now());
        Korisnik k = stari.getKorisnik();
        TokenResponse odgovor = new TokenResponse(jwtServis.napravi(k), KorisnikResponse.iz(k));
        return new Osvjezeno(odgovor, izdaj(k, stari.getObitelj()));
    }

    // Odjava: opoziva samo ovu prijavu, ostali uređaji ostaju prijavljeni
    @Transactional
    public void opozovi(String token) {
        repository.findByTokenHash(hash(token)).ifPresent(t -> repository.obrisiObitelj(t.getObitelj()));
    }

    // Nakon promjene lozinke: odjavi sve druge uređaje, a trenutni (ako je poznat) ostaje prijavljen
    @Transactional
    public void opozoviOstale(Long korisnikId, String trenutniToken) {
        String obitelj = trenutniToken == null ? null
                : repository.findByTokenHash(hash(trenutniToken)).map(RefreshToken::getObitelj).orElse(null);
        if (obitelj == null) {
            repository.obrisiSve(korisnikId);
        } else {
            repository.obrisiOstale(korisnikId, obitelj);
        }
    }

    @Transactional
    public void opozoviSve(Long korisnikId) {
        repository.obrisiSve(korisnikId);
    }

    @Scheduled(cron = "0 45 4 * * *", zone = "Europe/Zagreb")
    @Transactional
    public void obrisiIstekle() {
        repository.obrisiIstekle(Instant.now());
    }

    private String izdaj(Korisnik korisnik, String obitelj) {
        String token = TajniToken.novi();
        RefreshToken r = new RefreshToken();
        r.setKorisnik(korisnik);
        r.setTokenHash(hash(token));
        r.setObitelj(obitelj);
        r.setIstjece(Instant.now().plus(trajanje));
        repository.save(r);
        return token;
    }

    // Token je 32 nasumična bajta, pa je brzi SHA-256 dovoljan (BCrypt treba za lozinke koje se mogu pogađati)
    static String hash(String token) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ResponseStatusException neispravan() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Prijava je istekla. Prijavite se ponovno.");
    }
}
