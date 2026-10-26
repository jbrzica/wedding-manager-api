package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "klijent")
@Getter
@Setter
@NoArgsConstructor
public class Klijent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ime;

    @Column(nullable = false)
    private String prezime;

    @Column(nullable = false, unique = true)
    private String email;

    private String telefon;
}