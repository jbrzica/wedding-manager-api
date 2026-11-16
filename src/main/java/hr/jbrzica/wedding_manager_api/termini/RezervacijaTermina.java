package hr.jbrzica.wedding_manager_api.termini;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "rezervacija_termina")
@Getter
@Setter
@NoArgsConstructor
public class RezervacijaTermina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private DogadjajTermina dogadjaj;

    // Može nestati ako se paket obriše; naziv, trajanje i cijena su zato prepisani ovdje
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paket_id")
    private PaketTermina paket;

    @Column(name = "naziv_paketa", nullable = false)
    private String nazivPaketa;

    @Column(name = "trajanje_minuta", nullable = false)
    private int trajanjeMinuta;

    @Column(nullable = false)
    private BigDecimal cijena;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private LocalTime vrijeme;

    @Column(nullable = false)
    private String ime;

    @Column(nullable = false)
    private String telefon;

    private String email;

    private String napomena;

    @Column(name = "otkazni_hash", unique = true)
    private String otkazniHash;

    @Column(nullable = false)
    private boolean rucno;

    @Column(nullable = false)
    private Instant kreirano;

    private Instant otkazano;
}
