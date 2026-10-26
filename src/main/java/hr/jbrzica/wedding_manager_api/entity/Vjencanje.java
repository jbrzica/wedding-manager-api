package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

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
}