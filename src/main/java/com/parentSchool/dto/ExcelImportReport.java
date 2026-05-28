package com.parentSchool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportReport {
    private Integer lignesLues = 0;
    private Integer elevesCrees = 0;       // membres créés
    private Integer elevesMisAJour = 0;    // membres mis à jour
    private Integer tuteursCrees = 0;      // liens parrain créés
    private Integer lignesIgnorees = 0;
    private List<String> avertissements = new ArrayList<>();
}
