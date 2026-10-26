package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Klijent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KlijentRepository extends JpaRepository<Klijent, Long> {
}