package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Statistika: honorari se zbrajaju u bazi, jednim upitom za sva vjenčanja
    @Query("""
            SELECT new hr.jbrzica.wedding_manager_api.repository.ZbrojPoVjencanju(c.vjencanje.id, COALESCE(SUM(c.honorar), 0))
            FROM ClanEkipe c
            WHERE c.vjencanje.id IN :ids
            GROUP BY c.vjencanje.id
            """)
    List<ZbrojPoVjencanju> honorariPoVjencanju(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT new hr.jbrzica.wedding_manager_api.repository.ZbrojHonorara(c.suradnik.ime, COUNT(c), COALESCE(SUM(c.honorar), 0))
            FROM ClanEkipe c
            WHERE c.vjencanje.id IN :ids
            GROUP BY c.suradnik.id, c.suradnik.ime
            ORDER BY COALESCE(SUM(c.honorar), 0) DESC
            """)
    List<ZbrojHonorara> honorariPoSuradniku(@Param("ids") Collection<Long> ids);
}
