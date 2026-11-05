package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Šifra poslana mailom: za potvrdu emaila ili za poziv u tvrtku.
@Entity
@Table(name = "email_kod")
@Getter
@Setter
@NoArgsConstructor
public class EmailKod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // Kao i lozinka, šifra se sprema samo kao hash.
    @Column(nullable = false)
    private String kodHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SvrhaKoda svrha;

    // Samo kod poziva: u koju tvrtku se korisnik poziva.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tvrtka_id")
    private Tvrtka tvrtka;

    @Column(nullable = false)
    private Instant istjece;

    @Column(nullable = false)
    private int pokusaji;

    @Column(nullable = false)
    private boolean iskoristen;
}
