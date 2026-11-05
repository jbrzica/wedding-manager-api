package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ClanEkipeRepository extends JpaRepository<ClanEkipe, Long> {

    // Na kojim vjenčanjima tvrtke su ovi suradnici zadanog datuma
    List<ClanEkipe> findBySuradnikIdInAndVjencanjeDatumAndVjencanjeTvrtkaId(
            Collection<Long> suradnikIds, LocalDate datum, Long tvrtkaId);
}
