package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VjencanjeRepository extends JpaRepository<Vjencanje, Long> {

    List<Vjencanje> findAllByOrderByDatumAsc();

    List<Vjencanje> findByDatumGreaterThanEqualOrderByDatumAsc(LocalDate datum);

    List<Vjencanje> findByDatumLessThanOrderByDatumDesc(LocalDate datum);

    List<Vjencanje> findByDatumBetweenOrderByDatumAsc(LocalDate od, LocalDate doDatuma);

    List<Vjencanje> findByDatum(LocalDate datum);

    List<Vjencanje> findByStatusNot(StatusVjencanja status);

    Optional<Vjencanje> findFirstByDatumGreaterThanEqualAndStatusNotOrderByDatumAsc(LocalDate datum, StatusVjencanja status);

    long countByDatumBetweenAndStatusNot(LocalDate od, LocalDate doDatuma, StatusVjencanja status);
}
