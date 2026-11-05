package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Svaki upit traži i tvrtku: korisnik vidi samo vjenčanja svoje tvrtke.
public interface VjencanjeRepository extends JpaRepository<Vjencanje, Long> {

    Optional<Vjencanje> findByIdAndTvrtkaId(Long id, Long tvrtkaId);

    List<Vjencanje> findByTvrtkaIdOrderByDatumAsc(Long tvrtkaId);

    List<Vjencanje> findByTvrtkaIdAndDatumGreaterThanEqualOrderByDatumAsc(Long tvrtkaId, LocalDate datum);

    List<Vjencanje> findByTvrtkaIdAndDatumLessThanOrderByDatumDesc(Long tvrtkaId, LocalDate datum);

    List<Vjencanje> findByTvrtkaIdAndDatumBetweenOrderByDatumAsc(Long tvrtkaId, LocalDate od, LocalDate doDatuma);

    List<Vjencanje> findByTvrtkaIdAndDatum(Long tvrtkaId, LocalDate datum);

    List<Vjencanje> findByTvrtkaIdAndStatusNot(Long tvrtkaId, StatusVjencanja status);

    Optional<Vjencanje> findFirstByTvrtkaIdAndDatumGreaterThanEqualAndStatusNotOrderByDatumAsc(
            Long tvrtkaId, LocalDate datum, StatusVjencanja status);

    long countByTvrtkaIdAndDatumBetweenAndStatusNot(
            Long tvrtkaId, LocalDate od, LocalDate doDatuma, StatusVjencanja status);
}
