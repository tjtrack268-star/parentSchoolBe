package com.parentSchool.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tuteurs")
@Data
public class Tuteur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @OneToMany(mappedBy = "tuteur", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Eleve> eleves = new ArrayList<>();
}
