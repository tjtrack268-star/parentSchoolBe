package com.parentSchool.dto;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class OrganigrammeNode {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String sponsorshipCode;
    private String gradeName;
    private Integer directSponsorshipsCount;
    private Integer totalPoints;
    private List<OrganigrammeNode> children = new ArrayList<>();
    
    public OrganigrammeNode(Long id, String firstName, String lastName, String email, 
                           String sponsorshipCode, String gradeName, Integer directSponsorshipsCount, Integer totalPoints) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.sponsorshipCode = sponsorshipCode;
        this.gradeName = gradeName;
        this.directSponsorshipsCount = directSponsorshipsCount;
        this.totalPoints = totalPoints;
    }
}