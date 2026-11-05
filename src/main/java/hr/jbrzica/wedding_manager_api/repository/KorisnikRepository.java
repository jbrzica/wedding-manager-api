package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Korisnik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KorisnikRepository extends JpaRepository<Korisnik, Long> {

    Optional<Korisnik> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Korisnik> findByTvrtkaIdOrderByImeAsc(Long tvrtkaId);

    Optional<Korisnik> findByIdAndTvrtkaId(Long id, Long tvrtkaId);
}
