package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Podaci o jednoj osobi (mladi ili mladozenji).
 * Nije zasebna tablica: polja postaju stupci tablice vjencanje.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Osoba {

    private String ime;
    private String prezime;
    private String telefon;
    private String email;
    private String adresa;

    public String punoIme() {
        String i = ime == null ? "" : ime;
        String p = prezime == null ? "" : prezime;
        return (i + " " + p).trim();
    }
}
