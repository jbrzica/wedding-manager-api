package hr.jbrzica.wedding_manager_api.termini;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Svaki korisnik ima svoje "pročitano": mama i tata obavijest vide neovisno jedno o drugom
@Entity
@Table(name = "termini_vidjeno")
@Getter
@Setter
@NoArgsConstructor
public class TerminiVidjeno {

    @Id
    @Column(name = "korisnik_id")
    private Long korisnikId;

    @Column(name = "vidjeno_do", nullable = false)
    private Instant vidjenoDo;
}
