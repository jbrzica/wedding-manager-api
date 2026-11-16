package hr.jbrzica.wedding_manager_api.security;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtServis {

    private final SecretKey kljuc;
    private final long trajanjeMs;

    public JwtServis(@Value("${app.jwt.tajna}") String tajna,
                     @Value("${app.jwt.trajanje-minuta}") long trajanjeMinuta) {
        if (tajna == null || tajna.isBlank()) {
            throw new IllegalStateException("JWT_TAJNA nije postavljena. Postavi je kao varijablu okruženja (Base64, najmanje 32 bajta).");
        }
        byte[] bajtovi = Decoders.BASE64.decode(tajna.trim());
        // HS256 traži barem 256 bita; kraća tajna se može pogoditi
        if (bajtovi.length < 32) {
            throw new IllegalStateException("JWT_TAJNA je prekratka: treba najmanje 32 bajta, a ima " + bajtovi.length + ".");
        }
        this.kljuc = Keys.hmacShaKeyFor(bajtovi);
        // Kratko: ukradeni access token vrijedi samo par minuta, a prijavu drži refresh token
        this.trajanjeMs = trajanjeMinuta * 60 * 1000;
    }

    public String napravi(Korisnik korisnik) {
        Date sada = new Date();
        return Jwts.builder()
                .subject(korisnik.getId().toString())
                .issuedAt(sada)
                .expiration(new Date(sada.getTime() + trajanjeMs))
                .signWith(kljuc)
                .compact();
    }

    // Baci JwtException ako je token krivotvoren, oštećen ili istekao.
    public Long procitajKorisnikId(String token) {
        String subject = Jwts.parser()
                .verifyWith(kljuc)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }
}
