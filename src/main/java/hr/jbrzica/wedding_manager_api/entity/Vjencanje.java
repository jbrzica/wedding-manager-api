package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "vjencanje")
// Vjenčanja u košu ne vidi nijedan JPA upit; koš ih dohvaća posebnim SQL upitima u repozitoriju
@SQLRestriction("obrisano IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Vjencanje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kad je premješteno u koš; null = vjenčanje je aktivno
    private Instant obrisano;

    @Column(nullable = false)
    private LocalDate datum;

    private String lokacija;

    @Embedded
    @AttributeOverride(name = "ime", column = @Column(name = "mlada_ime"))
    @AttributeOverride(name = "prezime", column = @Column(name = "mlada_prezime"))
    @AttributeOverride(name = "telefon", column = @Column(name = "mlada_telefon"))
    @AttributeOverride(name = "email", column = @Column(name = "mlada_email"))
    @AttributeOverride(name = "adresa", column = @Column(name = "mlada_adresa"))
    @AttributeOverride(name = "oib", column = @Column(name = "mlada_oib"))
    private Osoba mlada;

    @Embedded
    @AttributeOverride(name = "ime", column = @Column(name = "mladozenja_ime"))
    @AttributeOverride(name = "prezime", column = @Column(name = "mladozenja_prezime"))
    @AttributeOverride(name = "telefon", column = @Column(name = "mladozenja_telefon"))
    @AttributeOverride(name = "email", column = @Column(name = "mladozenja_email"))
    @AttributeOverride(name = "adresa", column = @Column(name = "mladozenja_adresa"))
    @AttributeOverride(name = "oib", column = @Column(name = "mladozenja_oib"))
    private Osoba mladozenja;

    // Paket nije obavezan: ponekad se dogovori samo cijena.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paket_id")
    private Paket paket;

    @ManyToMany
    @JoinTable(
            name = "vjencanje_dodatak",
            joinColumns = @JoinColumn(name = "vjencanje_id"),
            inverseJoinColumns = @JoinColumn(name = "dodatak_id")
    )
    private Set<Dodatak> dodaci = new HashSet<>();

    // Kopija cijene u trenutku dogovora: kasnija promjena cijene paketa
    // ne smije promijeniti cijenu starih vjencanja.
    @Column(nullable = false)
    private BigDecimal dogovorenaCijena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusVjencanja status = StatusVjencanja.UPIT;

    private LocalDate rokIsporuke;

    @Enumerated(EnumType.STRING)
    private IzvorUpita izvorUpita;

    @Column(length = 4000)
    private String napomene;

    private Integer brojGostiju;

    // Smještaj, put, albumi... jedan zbroj koji korisnik sam upiše
    private BigDecimal ostaliTroskovi;

    @Column(length = 1000)
    private String linkGalerije;

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("vrijeme ASC")
    private List<StavkaRasporeda> raspored = new ArrayList<>();

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClanEkipe> ekipa = new ArrayList<>();

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("datum ASC")
    private List<Uplata> uplate = new ArrayList<>();

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("redoslijed ASC, id ASC")
    private List<Zadatak> zadaci = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tvrtka_id", nullable = false)
    private Tvrtka tvrtka;
}
