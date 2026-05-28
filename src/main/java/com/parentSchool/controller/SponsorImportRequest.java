package com.parentSchool.controller;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SponsorImportRequest {
    private List<SponsorNode> reseau_parrainage = new ArrayList<>();

    @Data
    public static class SponsorNode {
        private String id;
        private Integer num;
        private String code;
        private String profession;
        private String pays;
        private Boolean root;
        private List<SponsorNode> filleuls = new ArrayList<>();
    }
}

