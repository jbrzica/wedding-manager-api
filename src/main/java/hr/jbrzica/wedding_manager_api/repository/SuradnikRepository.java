package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Suradnik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuradnikRepository extends JpaRepository<Suradnik, Long> {

    List<Suradnik> findByTvrtkaIdOrderByImeAsc(Long tvrtkaId);

    Optional<Suradnik> findByIdAndTvrtkaId(Long id, Long tvrtkaId);
}
