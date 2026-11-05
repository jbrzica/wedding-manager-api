package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "vjencanje")
@Getter
@Setter
@NoArgsConstructor
public class Vjencanje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate datum;

    private String lokacija;

    @Embedded
    @AttributeOverride(name = "ime", column = @Column(name = "mlada_ime"))
    @AttributeOverride(name = "prezime", column = @Column(name = "mlada_prezime"))
    @AttributeOverride(name = "telefon", column = @Column(name = "mlada_telefon"))
    @AttributeOverride(name = "email", column = @Column(name = "mlada_email"))
    @AttributeOverride(name = "adresa", column = @Column(name = "mlada_adresa"))
    private Osoba mlada;

    @Embedded
    @AttributeOverride(name = "ime", column = @Column(name = "mladozenja_ime"))
    @AttributeOverride(name = "prezime", column = @Column(name = "mladozenja_prezime"))
    @AttributeOverride(name = "telefon", column = @Column(name = "mladozenja_telefon"))
    @AttributeOverride(name = "email", column = @Column(name = "mladozenja_email"))
    @AttributeOverride(name = "adresa", column = @Column(name = "mladozenja_adresa"))
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

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("vrijeme ASC")
    private List<StavkaRasporeda> raspored = new ArrayList<>();

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClanEkipe> ekipa = new ArrayList<>();

    @OneToMany(mappedBy = "vjencanje", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("datum ASC")
    private List<Uplata> uplate = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tvrtka_id", nullable = false)
    private Tvrtka tvrtka;
}
