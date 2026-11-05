package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DodatakRepository extends JpaRepository<Dodatak, Long> {

    List<Dodatak> findByTvrtkaIdOrderByNazivAsc(Long tvrtkaId);

    Optional<Dodatak> findByIdAndTvrtkaId(Long id, Long tvrtkaId);

    List<Dodatak> findByIdInAndTvrtkaId(Collection<Long> ids, Long tvrtkaId);
}
