package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Svaki upit traži i tvrtku: korisnik vidi samo vjenčanja svoje tvrtke.
public interface VjencanjeRepository extends JpaRepository<Vjencanje, Long> {

    Optional<Vjencanje> findByIdAndTvrtkaId(Long id, Long tvrtkaId);

    long countByTvrtkaId(Long tvrtkaId);

    List<Vjencanje> findByTvrtkaIdOrderByDatumAsc(Long tvrtkaId);

    List<Vjencanje> findByTvrtkaIdAndDatumGreaterThanEqualOrderByDatumAsc(Long tvrtkaId, LocalDate datum);

    List<Vjencanje> findByTvrtkaIdAndDatumLessThanOrderByDatumDesc(Long tvrtkaId, LocalDate datum);

    List<Vjencanje> findByTvrtkaIdAndDatumBetweenOrderByDatumAsc(Long tvrtkaId, LocalDate od, LocalDate doDatuma);

    List<Vjencanje> findByTvrtkaIdAndDatum(Long tvrtkaId, LocalDate datum);

    List<Vjencanje> findByTvrtkaIdAndStatusNotIn(Long tvrtkaId, Collection<StatusVjencanja> statusi);

    Optional<Vjencanje> findFirstByTvrtkaIdAndDatumGreaterThanEqualAndStatusNotInOrderByDatumAsc(
            Long tvrtkaId, LocalDate datum, Collection<StatusVjencanja> statusi);

    long countByTvrtkaIdAndDatumBetweenAndStatusNotIn(
            Long tvrtkaId, LocalDate od, LocalDate doDatuma, Collection<StatusVjencanja> statusi);

    // ---------- koš ----------
    // Obični upiti ne vide obrisana vjenčanja (@SQLRestriction na entitetu), pa koš koristi izravan SQL.

    @Query(value = "SELECT * FROM vjencanje WHERE tvrtka_id = :tvrtkaId AND obrisano IS NOT NULL ORDER BY obrisano DESC",
            nativeQuery = true)
    List<Vjencanje> uKosu(@Param("tvrtkaId") Long tvrtkaId);

    @Modifying
    @Query(value = "UPDATE vjencanje SET obrisano = NULL WHERE id = :id AND tvrtka_id = :tvrtkaId AND obrisano IS NOT NULL",
            nativeQuery = true)
    int vratiIzKosa(@Param("id") Long id, @Param("tvrtkaId") Long tvrtkaId);

    // Raspored, ekipa, uplate i zadaci brišu se u bazi zajedno s vjenčanjem (ON DELETE CASCADE)
    @Modifying
    @Query(value = "DELETE FROM vjencanje WHERE id = :id AND tvrtka_id = :tvrtkaId AND obrisano IS NOT NULL",
            nativeQuery = true)
    int obrisiTrajno(@Param("id") Long id, @Param("tvrtkaId") Long tvrtkaId);

    @Modifying
    @Query(value = "DELETE FROM vjencanje WHERE obrisano < :granica", nativeQuery = true)
    int obrisiStarijaOd(@Param("granica") Instant granica);
}
