package com.parentSchool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EleveNode {
    private Long id;
    private String nomComplet;
    private String codeMembre;
    private String pays;
}
