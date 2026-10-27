package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VjencanjeRepository extends JpaRepository<Vjencanje, Long> {

    List<Vjencanje> findByStatus(StatusVjencanja status);
}