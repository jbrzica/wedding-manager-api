package hr.jbrzica.wedding_manager_api.repository;

import hr.jbrzica.wedding_manager_api.dto.Razdoblje;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filteri popisa vjenčanja kao jedan SQL upit. Moraju biti u bazi, a ne u Javi,
 * jer se popis dohvaća po stranicama: baza mora znati koliko ih ukupno odgovara.
 */
public record VjencanjeFilter(
        Long tvrtkaId,
        Razdoblje razdoblje,
        Integer godina,
        List<StatusVjencanja> statusi,
        String pretraga,
        boolean nenaplaceno,
        boolean kasniIsporuka,
        boolean bezKapare,
        LocalDate danas
) {

    public Specification<Vjencanje> uSpecifikaciju() {
        return (v, upit, cb) -> {
            List<Predicate> uvjeti = new ArrayList<>();
            uvjeti.add(cb.equal(v.get("tvrtka").get("id"), tvrtkaId));

            Path<LocalDate> datum = v.get("datum");
            if (razdoblje == Razdoblje.NADOLAZECA) {
                uvjeti.add(cb.greaterThanOrEqualTo(datum, danas));
            } else if (razdoblje == Razdoblje.PROSLA) {
                uvjeti.add(cb.lessThan(datum, danas));
            }
            if (godina != null) {
                uvjeti.add(cb.between(datum, LocalDate.of(godina, 1, 1), LocalDate.of(godina, 12, 31)));
            }
            if (statusi != null && !statusi.isEmpty()) {
                uvjeti.add(v.get("status").in(statusi));
            }
            if (pretraga != null && !pretraga.isBlank()) {
                String uzorak = "%" + pretraga.trim().toLowerCase() + "%";
                uvjeti.add(cb.or(
                        cb.like(cb.lower(punoIme(cb, v, "mlada")), uzorak),
                        cb.like(cb.lower(punoIme(cb, v, "mladozenja")), uzorak),
                        cb.like(cb.lower(cb.coalesce(v.get("lokacija"), "")), uzorak)));
            }
            if (kasniIsporuka) {
                // Vjenčanje je bilo, rok je prošao, a fotografije još nisu predane. Otkazana ne kasne.
                uvjeti.add(cb.lessThan(datum, danas));
                uvjeti.add(cb.lessThan(v.get("rokIsporuke"), danas));
                uvjeti.add(cb.not(v.get("status").in(StatusVjencanja.ISPORUCENO, StatusVjencanja.OTKAZANO)));
            }
            if (bezKapare) {
                // Potvrđeno je i tek dolazi, a nijedna uplata nije kapara
                Subquery<Long> kapara = upit.subquery(Long.class);
                Root<Uplata> u = kapara.from(Uplata.class);
                kapara.select(u.get("id"))
                        .where(cb.equal(u.get("vjencanje"), v), cb.equal(u.get("vrsta"), VrstaUplate.KAPARA));
                uvjeti.add(cb.greaterThanOrEqualTo(datum, danas));
                uvjeti.add(cb.equal(v.get("status"), StatusVjencanja.POTVRDENO));
                uvjeti.add(cb.not(cb.exists(kapara)));
            }
            if (nenaplaceno) {
                Subquery<BigDecimal> uplaceno = upit.subquery(BigDecimal.class);
                Root<Uplata> u = uplaceno.from(Uplata.class);
                uplaceno.select(cb.coalesce(cb.sum(u.get("iznos")), BigDecimal.ZERO))
                        .where(cb.equal(u.get("vjencanje"), v));
                uvjeti.add(cb.greaterThan(v.get("dogovorenaCijena"), uplaceno));
                uvjeti.add(cb.notEqual(v.get("status"), StatusVjencanja.OTKAZANO));
            }
            return cb.and(uvjeti.toArray(Predicate[]::new));
        };
    }

    // "Ana Horvat", isto kao Osoba.punoIme(), da se može tražiti i po imenu i prezimenu zajedno
    private static Expression<String> punoIme(CriteriaBuilder cb, Root<Vjencanje> v, String osoba) {
        Expression<String> ime = cb.coalesce(v.get(osoba).get("ime"), "");
        Expression<String> prezime = cb.coalesce(v.get(osoba).get("prezime"), "");
        return cb.concat(cb.concat(ime, " "), prezime);
    }
}
