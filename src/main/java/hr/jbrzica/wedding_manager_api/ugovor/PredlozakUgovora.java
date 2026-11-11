package hr.jbrzica.wedding_manager_api.ugovor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Vlastiti Word predložak jedne tvrtke; najviše jedan po tvrtki, zato je tvrtka_id ujedno i ključ
@Entity
@Table(name = "predlozak_ugovora")
@Getter
@Setter
@NoArgsConstructor
public class PredlozakUgovora {

    @Id
    @Column(name = "tvrtka_id")
    private Long tvrtkaId;

    @Column(nullable = false)
    private String nazivDatoteke;

    @Column(nullable = false)
    private byte[] sadrzaj;

    @Column(nullable = false)
    private Instant azurirano;
}
