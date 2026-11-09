package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Paket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Svaki upit traži i tvrtku: korisnik vidi samo podatke svoje tvrtke.
public interface PaketRepository extends JpaRepository<Paket, Long> {

    List<Paket> findByTvrtkaIdOrderByNazivAsc(Long tvrtkaId);

    Optional<Paket> findByIdAndTvrtkaId(Long id, Long tvrtkaId);

    long countByTvrtkaId(Long tvrtkaId);
}
