package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UplataRepository extends JpaRepository<Uplata, Long> {

    List<Uplata> findByVjencanjeIdOrderByDatumAsc(Long vjencanjeId);

    // Jedan upit za zbroj uplata svih vjencanja u popisu (umjesto jednog upita po vjencanju)
    @Query("""
            SELECT new hr.jbrzica.wedding_manager_api.repository.ZbrojUplata(u.vjencanje.id, SUM(u.iznos))
            FROM Uplata u
            WHERE u.vjencanje.id IN :ids
            GROUP BY u.vjencanje.id
            """)
    List<ZbrojUplata> zbrojiPoVjencanju(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT DISTINCT u.vjencanje.id
            FROM Uplata u
            WHERE u.vrsta = :vrsta AND u.vjencanje.id IN :ids
            """)
    List<Long> vjencanjaSUplatomVrste(@Param("vrsta") VrstaUplate vrsta, @Param("ids") Collection<Long> ids);
}
