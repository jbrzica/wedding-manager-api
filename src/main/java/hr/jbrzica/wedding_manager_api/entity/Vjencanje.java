package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
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

    @Column(nullable = false)
    private String lokacija;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusVjencanja status = StatusVjencanja.UPIT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "klijent_id", nullable = false)
    private Klijent klijent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paket_id", nullable = false)
    private Paket paket;

    @ManyToMany
    @JoinTable(
            name = "vjencanje_dodatak",
            joinColumns = @JoinColumn(name = "vjencanje_id"),
            inverseJoinColumns = @JoinColumn(name = "dodatak_id")
    )
    private Set<Dodatak> dodaci = new HashSet<>();
}