package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Jedna kvačica s kontrolne liste vjenčanja, npr. "Ugovor potpisan"
@Entity
@Table(name = "zadatak")
@Getter
@Setter
@NoArgsConstructor
public class Zadatak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    private boolean gotovo;

    private int redoslijed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vjencanje_id", nullable = false)
    private Vjencanje vjencanje;
}
