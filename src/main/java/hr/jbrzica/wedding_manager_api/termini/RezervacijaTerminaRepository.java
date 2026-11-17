package hr.jbrzica.wedding_manager_api.termini;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RezervacijaTerminaRepository extends JpaRepository<RezervacijaTermina, Long> {

    List<RezervacijaTermina> findByDogadjajIdAndOtkazanoIsNullOrderByDatumAscVrijemeAsc(Long dogadjajId);

    List<RezervacijaTermina> findByDogadjajIdInAndOtkazanoIsNull(List<Long> dogadjajIds);

    // Koliko aktivnih termina isti email već ima na ovom događaju
    @Query("""
            SELECT COUNT(r) FROM RezervacijaTermina r
            WHERE r.dogadjaj.id = :dogadjajId AND r.otkazano IS NULL AND LOWER(r.email) = LOWER(:email)
            """)
    long aktivnihNaEmail(@Param("dogadjajId") Long dogadjajId, @Param("email") String email);

    Optional<RezervacijaTermina> findByIdAndDogadjajTvrtkaId(Long id, Long tvrtkaId);

    Optional<RezervacijaTermina> findByOtkazniHash(String otkazniHash);

    // Online rezervacije tvrtke nakon zadanog trenutka (ručno upisane studio ionako zna)
    @Query("""
            SELECT r FROM RezervacijaTermina r JOIN FETCH r.dogadjaj d
            WHERE d.tvrtka.id = :tvrtkaId AND r.otkazano IS NULL AND r.rucno = false AND r.kreirano > :od
            ORDER BY r.kreirano DESC
            """)
    List<RezervacijaTermina> noveOd(@Param("tvrtkaId") Long tvrtkaId, @Param("od") java.time.Instant od);
}
