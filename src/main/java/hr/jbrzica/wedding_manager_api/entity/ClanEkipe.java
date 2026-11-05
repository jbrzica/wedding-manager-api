package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Suradnik na jednom vjencanju, s ulogom i honorarom.
 * Spaja vjencanje i suradnika, ali ima i svoje podatke,
 * zato je pravi entitet, a ne obican @ManyToMany.
 */
@Entity
@Table(name = "clan_ekipe")
@Getter
@Setter
@NoArgsConstructor
public class ClanEkipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vjencanje_id", nullable = false)
    private Vjencanje vjencanje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suradnik_id", nullable = false)
    private Suradnik suradnik;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UlogaSuradnika uloga;

    private BigDecimal honorar;
}
