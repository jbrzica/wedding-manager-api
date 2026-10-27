package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "dodatak")
@Getter
@Setter
@NoArgsConstructor
public class Dodatak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Naziv je obavezan")
    @Column(nullable = false)
    private String naziv;

    @NotNull(message = "Cijena je obavezna")
    @Positive(message = "Cijena mora biti veća od 0")
    @Column(nullable = false)
    private BigDecimal cijena;
}