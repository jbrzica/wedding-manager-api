package hr.jbrzica.wedding_manager_api.termini;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DogadjajTerminaRepository extends JpaRepository<DogadjajTermina, Long> {

    Optional<DogadjajTermina> findByIdAndTvrtkaId(Long id, Long tvrtkaId);

    List<DogadjajTermina> findByTvrtkaIdOrderByKreiranoDesc(Long tvrtkaId);

    Optional<DogadjajTermina> findByJavniKod(String javniKod);
}
