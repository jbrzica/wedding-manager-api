package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Suradnik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuradnikRepository extends JpaRepository<Suradnik, Long> {

    List<Suradnik> findAllByOrderByImeAsc();
}
