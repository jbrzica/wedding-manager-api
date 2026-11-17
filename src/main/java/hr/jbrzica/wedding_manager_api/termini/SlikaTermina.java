package hr.jbrzica.wedding_manager_api.termini;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Naslovna slika jednog fotografiranja; najviše jedna, zato je id događaja ujedno i ključ
@Entity
@Table(name = "slika_termina")
@Getter
@Setter
@NoArgsConstructor
public class SlikaTermina {

    @Id
    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    @Column(nullable = false)
    private String tip;

    @Column(nullable = false)
    private byte[] sadrzaj;

    @Column(nullable = false)
    private Instant azurirano;
}
