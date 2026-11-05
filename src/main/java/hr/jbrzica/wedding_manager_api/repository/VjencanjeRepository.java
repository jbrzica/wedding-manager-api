package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
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

    List<Vjencanje> findByTvrtkaIdAndStatusNotIn(Long tvrtkaId, Collection<StatusVjencanja> statusi);

    Optional<Vjencanje> findFirstByTvrtkaIdAndDatumGreaterThanEqualAndStatusNotInOrderByDatumAsc(
            Long tvrtkaId, LocalDate datum, Collection<StatusVjencanja> statusi);

    long countByTvrtkaIdAndDatumBetweenAndStatusNotIn(
            Long tvrtkaId, LocalDate od, LocalDate doDatuma, Collection<StatusVjencanja> statusi);
}
