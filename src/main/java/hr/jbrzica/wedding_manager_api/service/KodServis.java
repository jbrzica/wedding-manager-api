package hr.jbrzica.wedding_manager_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hr.jbrzica.wedding_manager_api.entity.EmailKod;
import hr.jbrzica.wedding_manager_api.entity.SvrhaKoda;
import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import hr.jbrzica.wedding_manager_api.repository.EmailKodRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class KodServis {

    private static final int TRAJANJE_MINUTA = 15;
    private static final int MAX_POKUSAJA = 5;
    // Najviše šifri na isti email u sat vremena. Bez granice bi se preko aplikacije moglo
    // zatrpati nečiji sandučić, trošiti Brevo kvota i dobivati nove pokušaje pogađanja.
    static final int MAX_SIFRI_NA_SAT = 5;
    private static final Logger log = LoggerFactory.getLogger(KodServis.class);

    private final EmailKodRepository emailKodRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServis emailServis;
    private final SecureRandom random = new SecureRandom();

    public KodServis(EmailKodRepository emailKodRepository, PasswordEncoder passwordEncoder,
                     EmailServis emailServis) {
        this.emailKodRepository = emailKodRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailServis = emailServis;
    }

    @Transactional
    public void posaljiKod(String email, SvrhaKoda svrha, Tvrtka tvrtka, String naslov, String uvod) {
        Instant prijeSat = Instant.now().minus(60 - TRAJANJE_MINUTA, ChronoUnit.MINUTES);
        if (emailKodRepository.countByEmailAndIstjeceAfter(email, prijeSat) >= MAX_SIFRI_NA_SAT) {
            // Tiho: poruka o grešci bi otkrila postoji li račun s tim emailom
            log.warn("Preskočeno slanje šifre: previše zahtjeva za isti email u sat vremena");
            return;
        }
        // Nova šifra poništava sve stare za isti email i svrhu
        for (EmailKod stari : emailKodRepository.findByEmailAndSvrhaAndIskoristenFalse(email, svrha)) {
            stari.setIskoristen(true);
        }

        String kod = String.format("%06d", random.nextInt(1_000_000));

        EmailKod emailKod = new EmailKod();
        emailKod.setEmail(email);
        emailKod.setKodHash(passwordEncoder.encode(kod));
        emailKod.setSvrha(svrha);
        emailKod.setTvrtka(tvrtka);
        emailKod.setIstjece(Instant.now().plus(TRAJANJE_MINUTA, ChronoUnit.MINUTES));
        emailKodRepository.save(emailKod);

        emailServis.posalji(email, naslov,
                uvod + "\n\nVaša šifra: " + kod + "\n\nŠifra vrijedi " + TRAJANJE_MINUTA + " minuta.");
    }

    /**
     * Provjerava šifru i označava je iskorištenom.
     * Radi u svojoj transakciji (REQUIRES_NEW) koja se ne poništava kod krive šifre:
     * tako se broj pokušaja spremi i nakon greške, pa nitko ne može pogađati šifru unedogled.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public EmailKod provjeri(String email, SvrhaKoda svrha, String kod) {
        EmailKod emailKod = emailKodRepository
                .findFirstByEmailAndSvrhaAndIskoristenFalseOrderByIdDesc(email, svrha)
                .orElseThrow(KodServis::neispravnaSifra);

        if (emailKod.getIstjece().isBefore(Instant.now()) || emailKod.getPokusaji() >= MAX_POKUSAJA) {
            throw neispravnaSifra();
        }

        if (!passwordEncoder.matches(kod, emailKod.getKodHash())) {
            emailKod.setPokusaji(emailKod.getPokusaji() + 1);
            throw neispravnaSifra();
        }

        emailKod.setIskoristen(true);
        return emailKod;
    }

    private static ResponseStatusException neispravnaSifra() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Šifra nije ispravna ili je istekla. Zatražite novu šifru.");
    }
}
