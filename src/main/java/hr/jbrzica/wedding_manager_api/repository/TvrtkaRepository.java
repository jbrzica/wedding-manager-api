package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TvrtkaRepository extends JpaRepository<Tvrtka, Long> {

    Optional<Tvrtka> findByKalendarToken(String kalendarToken);
}
