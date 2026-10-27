package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Ime je obavezno")
    @Column(nullable = false)
    private String ime;

    @NotBlank(message = "Prezime je obavezno")
    @Column(nullable = false)
    private String prezime;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email nije ispravan")
    @Column(nullable = false, unique = true)
    private String email;

    private String telefon;
}