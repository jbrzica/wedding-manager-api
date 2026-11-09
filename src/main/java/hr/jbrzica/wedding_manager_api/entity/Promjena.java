package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Jedan zapis u povijesti promjena; imena se kopiraju, da zapis ostane čitljiv i kad osoba ili vjenčanje nestanu
@Entity
@Table(name = "promjena")
@Getter
@Setter
@NoArgsConstructor
public class Promjena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tvrtka_id", nullable = false)
    private Long tvrtkaId;

    @Column(name = "vjencanje_id")
    private Long vjencanjeId;

    private String vjencanjeNaziv;

    @Column(name = "korisnik_id")
    private Long korisnikId;

    @Column(nullable = false)
    private String korisnikIme;

    @Column(nullable = false)
    private Instant vrijeme;

    @Column(nullable = false, length = 500)
    private String opis;
}
