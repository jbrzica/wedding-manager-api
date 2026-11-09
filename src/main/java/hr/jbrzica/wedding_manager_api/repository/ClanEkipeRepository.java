package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ClanEkipeRepository extends JpaRepository<ClanEkipe, Long> {

    // Na kojim vjenčanjima tvrtke su ovi suradnici zadanog datuma
    List<ClanEkipe> findBySuradnikIdInAndVjencanjeDatumAndVjencanjeTvrtkaIdAndVjencanjeObrisanoIsNull(
            Collection<Long> suradnikIds, LocalDate datum, Long tvrtkaId);

    // Tko iz tvrtke radi zadanog datuma (za provjeru datuma)
    List<ClanEkipe> findByVjencanjeDatumAndVjencanjeTvrtkaIdAndVjencanjeObrisanoIsNull(LocalDate datum, Long tvrtkaId);

    // Sva mjesta u ekipi jednog suradnika, za njegovu stranicu i kalendar
    // Vjenčanja u košu se ne vide, jer ova veza ne prolazi kroz @SQLRestriction na Vjencanje
    List<ClanEkipe> findBySuradnikIdAndVjencanjeObrisanoIsNullOrderByVjencanjeDatumAsc(Long suradnikId);
}
