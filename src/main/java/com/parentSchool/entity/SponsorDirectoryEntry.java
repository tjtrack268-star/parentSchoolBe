package com.parentSchool.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sponsor_directory")
@Data
public class SponsorDirectoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "sponsor_code", unique = true)
    private String sponsorCode;

    @Column(name = "profession")
    private String profession;

    @Column(name = "country")
    private String country;

    @Column(name = "is_root")
    private boolean rootNode;
}

