package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suradnik")
@Getter
@Setter
@NoArgsConstructor
public class Suradnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UlogaSuradnika uloga;

    private String telefon;

    private String email;

    @Column(length = 2000)
    private String napomena;
}
