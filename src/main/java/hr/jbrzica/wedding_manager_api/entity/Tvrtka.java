package hr.jbrzica.wedding_manager_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tvrtka")
@Getter
@Setter
@NoArgsConstructor
public class Tvrtka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String naziv;

    @Column(unique = true, length = 64)
    private String kalendarToken;
}
