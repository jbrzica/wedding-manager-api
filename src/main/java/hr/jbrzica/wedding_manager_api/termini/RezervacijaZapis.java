package hr.jbrzica.wedding_manager_api.termini;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Spremanje rezervacije, zajedničko za javnu i ručnu. Je li termin slobodan ne provjerava se unaprijed:
 * baza ima jedinstveni indeks na (događaj, datum, vrijeme) za aktivne rezervacije, pa od dva istovremena
 * zahtjeva za isti termin prođe samo prvi, a drugi dobije 409.
 */
@Component
class RezervacijaZapis {

    private final RezervacijaTerminaRepository repository;

    RezervacijaZapis(RezervacijaTerminaRepository repository) {
        this.repository = repository;
    }

    RezervacijaTermina spremi(DogadjajTermina d, PaketTermina paket, LocalDate datum, LocalTime vrijeme, String ime,
                              String telefon, String email, String napomena, boolean rucno, String otkazniHash) {
        RezervacijaTermina r = new RezervacijaTermina();
        r.setDogadjaj(d);
        r.setPaket(paket);
        r.setNazivPaketa(paket.getNaziv());
        r.setTrajanjeMinuta(paket.getTrajanjeMinuta());
        r.setCijena(paket.getCijena());
        r.setDatum(datum);
        r.setVrijeme(vrijeme);
        r.setIme(ime.trim());
        r.setTelefon(TerminiService.prazno(telefon) == null ? "" : telefon.trim());
        r.setEmail(TerminiService.prazno(email));
        r.setNapomena(TerminiService.prazno(napomena));
        r.setRucno(rucno);
        r.setOtkazniHash(otkazniHash);
        r.setKreirano(Instant.now());
        try {
            // flush odmah, da se kršenje jedinstvenog indeksa vidi ovdje, a ne tek na kraju transakcije
            return repository.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Taj termin je upravo netko rezervirao. Odaberi drugo vrijeme.");
        }
    }
}
