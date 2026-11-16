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

@Entity
@Table(name = "paket_termina")
@Getter
@Setter
@NoArgsConstructor
public class PaketTermina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private DogadjajTermina dogadjaj;

    @Column(nullable = false)
    private String naziv;

    private String opis;

    @Column(name = "trajanje_minuta", nullable = false)
    private int trajanjeMinuta;

    @Column(nullable = false)
    private BigDecimal cijena;

    @Column(nullable = false)
    private int redoslijed;
}
