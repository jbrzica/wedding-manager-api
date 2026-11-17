package hr.jbrzica.wedding_manager_api.termini;

import hr.jbrzica.wedding_manager_api.entity.Tvrtka;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Jedno kratko fotografiranje s terminima (npr. Božićno fotografiranje), odvojeno od vjenčanja
@Entity
@Table(name = "dogadjaj_termina")
@Getter
@Setter
@NoArgsConstructor
public class DogadjajTermina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tvrtka_id", nullable = false)
    private Tvrtka tvrtka;

    @Column(nullable = false)
    private String naziv;

    private String opis;

    private String lokacija;

    @Column(name = "korak_minuta", nullable = false)
    private int korakMinuta = 30;

    @Column(name = "javni_kod", nullable = false, unique = true)
    private String javniKod;

    @Column(nullable = false)
    private boolean otvoren = true;

    // npr. "bozicno"; null = nema kratkog linka
    @Column(name = "kratki_link")
    private String kratkiLink;

    // 0 = nema slike
    @Column(name = "slika_verzija", nullable = false)
    private int slikaVerzija;

    @Column(nullable = false)
    private Instant kreirano;

    @OneToMany(mappedBy = "dogadjaj", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("redoslijed ASC, id ASC")
    private List<PaketTermina> paketi = new ArrayList<>();

    @OneToMany(mappedBy = "dogadjaj", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("datum ASC, od ASC")
    private List<DanTermina> dani = new ArrayList<>();
}
