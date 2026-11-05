package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Zadatak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZadatakRepository extends JpaRepository<Zadatak, Long> {

    Optional<Zadatak> findByIdAndVjencanjeId(Long id, Long vjencanjeId);
}
