package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "eleves")
@Data
public class Eleve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ligne")
    private Integer numeroLigne;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "code_membre", unique = true)
    private String codeMembre;

    private String profession;

    private String pays;

    @Column(name = "date_inscription")
    private LocalDate dateInscription;

    @ManyToOne
    @JoinColumn(name = "tuteur_id")
    private Tuteur tuteur;
}
