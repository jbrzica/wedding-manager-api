package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DodatakRepository extends JpaRepository<Dodatak, Long> {

    List<Dodatak> findAllByOrderByNazivAsc();
}
