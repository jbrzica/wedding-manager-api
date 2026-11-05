package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "korisnik")
@Getter
@Setter
@NoArgsConstructor
public class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ime;

    @Column(nullable = false, unique = true)
    private String email;

    // Nikad se ne sprema sama lozinka, samo njezin BCrypt hash.
    @Column(nullable = false)
    private String lozinkaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UlogaKorisnika uloga;

    @Column(nullable = false)
    private boolean emailPotvrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tvrtka_id", nullable = false)
    private Tvrtka tvrtka;
}
