package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Promjena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;

public interface PromjenaRepository extends JpaRepository<Promjena, Long> {

    List<Promjena> findByTvrtkaIdAndVjencanjeIdOrderByVrijemeDescIdDesc(Long tvrtkaId, Long vjencanjeId);

    List<Promjena> findByTvrtkaIdOrderByVrijemeDescIdDesc(Long tvrtkaId, Pageable stranica);

    List<Promjena> findByTvrtkaIdAndKorisnikIdOrderByVrijemeDescIdDesc(Long tvrtkaId, Long korisnikId, Pageable stranica);

    @Modifying
    @Query("DELETE FROM Promjena p WHERE p.vrijeme < :granica")
    int obrisiStarijeOd(@Param("granica") Instant granica);
}
