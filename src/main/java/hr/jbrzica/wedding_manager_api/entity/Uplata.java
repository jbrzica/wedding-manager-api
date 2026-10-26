package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "uplata")
@Getter
@Setter
@NoArgsConstructor
public class Uplata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal iznos;

    @Column(nullable = false)
    private LocalDate datum;

    private String opis;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vjencanje_id", nullable = false)
    private Vjencanje vjencanje;
}