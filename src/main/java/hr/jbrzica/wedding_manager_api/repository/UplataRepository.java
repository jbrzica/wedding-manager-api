package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Uplata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UplataRepository extends JpaRepository<Uplata, Long> {

    List<Uplata> findByVjencanjeId(Long vjencanjeId);
}