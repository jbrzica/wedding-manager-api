package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

// Jedna stavka tipičnog rasporeda tvrtke (npr. 15:00 Crkva)
@Entity
@Table(name = "predlozak_stavka")
@Getter
@Setter
@NoArgsConstructor
public class PredlozakStavka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tvrtka_id", nullable = false)
    private Tvrtka tvrtka;

    @Column(nullable = false)
    private int redoslijed;

    private LocalTime vrijeme;

    @Column(nullable = false)
    private String naziv;

    private String adresa;

    @Column(length = 2000)
    private String napomena;
}
