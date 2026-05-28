package com.parentSchool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuteurOrganigrammeNode {
    private Long tuteurId;
    private String nomTuteur;
    private Integer nombreEleves;
    private List<EleveNode> eleves = new ArrayList<>();
}
