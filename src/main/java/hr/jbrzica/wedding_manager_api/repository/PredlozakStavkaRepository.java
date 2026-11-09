package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.PredlozakStavka;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PredlozakStavkaRepository extends JpaRepository<PredlozakStavka, Long> {

    List<PredlozakStavka> findByTvrtkaIdOrderByRedoslijedAsc(Long tvrtkaId);

    @Modifying
    void deleteByTvrtkaId(Long tvrtkaId);
}
