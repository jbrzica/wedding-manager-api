package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "stavka_rasporeda")
@Getter
@Setter
@NoArgsConstructor
public class StavkaRasporeda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalTime vrijeme;

    @Column(nullable = false)
    private String naziv;

    private String adresa;

    @Column(length = 2000)
    private String napomena;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vjencanje_id", nullable = false)
    private Vjencanje vjencanje;
}
