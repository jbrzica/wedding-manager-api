package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.entity.EmailKod;
import hr.jbrzica.wedding_manager_api.entity.SvrhaKoda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailKodRepository extends JpaRepository<EmailKod, Long> {

    Optional<EmailKod> findFirstByEmailAndSvrhaAndIskoristenFalseOrderByIdDesc(String email, SvrhaKoda svrha);

    List<EmailKod> findByEmailAndSvrhaAndIskoristenFalse(String email, SvrhaKoda svrha);
}
