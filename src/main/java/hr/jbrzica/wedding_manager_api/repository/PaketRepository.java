package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Paket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaketRepository extends JpaRepository<Paket, Long> {
        }