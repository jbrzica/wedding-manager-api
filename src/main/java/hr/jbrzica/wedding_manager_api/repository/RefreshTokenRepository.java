package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.obitelj = :obitelj")
    int obrisiObitelj(@Param("obitelj") String obitelj);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.korisnik.id = :korisnikId AND r.obitelj <> :osimObitelji")
    int obrisiOstale(@Param("korisnikId") Long korisnikId, @Param("osimObitelji") String osimObitelji);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.korisnik.id = :korisnikId")
    int obrisiSve(@Param("korisnikId") Long korisnikId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.istjece < :granica")
    int obrisiIstekle(@Param("granica") Instant granica);
}
