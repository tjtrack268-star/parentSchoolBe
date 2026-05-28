package com.parentSchool.controller;

import com.parentSchool.enums.UserType;
import lombok.Data;

import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private UserType userType;
    private String city;
    private String country;
    private Long sponsorId;
    
    @Size(max = 16, message = "Code de parrainage invalide")
    private String sponsorCode;
    private String sponsorName;
}
