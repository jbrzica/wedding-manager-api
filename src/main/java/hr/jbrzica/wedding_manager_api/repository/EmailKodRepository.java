package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.EmailKod;
import hr.jbrzica.wedding_manager_api.entity.SvrhaKoda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

public interface EmailKodRepository extends JpaRepository<EmailKod, Long> {

    // Zaključava redak: dva istovremena pokušaja ne mogu oba proći istu šifru ni zaobići brojač pokušaja
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailKod> findFirstByEmailAndSvrhaAndIskoristenFalseOrderByIdDesc(String email, SvrhaKoda svrha);

    // Šifra vrijedi 15 minuta, pa istjece > (sada - 45 min) znači "poslana u zadnjih sat vremena"
    long countByEmailAndIstjeceAfter(String email, Instant granica);

    List<EmailKod> findByEmailAndSvrhaAndIskoristenFalse(String email, SvrhaKoda svrha);
}
