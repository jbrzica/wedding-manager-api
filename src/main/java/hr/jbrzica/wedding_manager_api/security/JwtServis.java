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
        this.kljuc = Keys.hmacShaKeyFor(Decoders.BASE64.decode(tajna));
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
