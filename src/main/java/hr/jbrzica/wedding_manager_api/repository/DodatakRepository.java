package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DodatakRepository extends JpaRepository<Dodatak, Long> {
}