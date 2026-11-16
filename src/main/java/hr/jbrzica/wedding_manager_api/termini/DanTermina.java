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

import java.time.LocalDate;
import java.time.LocalTime;

// Dan na koji se slika i od kada do kada
@Entity
@Table(name = "dan_termina")
@Getter
@Setter
@NoArgsConstructor
public class DanTermina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private DogadjajTermina dogadjaj;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private LocalTime od;

    // "do" je rezervirana riječ u SQL-u
    @Column(name = "do_vremena", nullable = false)
    private LocalTime doVremena;
}
