package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "paket")
@Getter
@Setter
@NoArgsConstructor
public class Paket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    private String opis;

    @Column(nullable = false)
    private BigDecimal cijena;
}