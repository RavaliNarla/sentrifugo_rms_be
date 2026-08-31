package com.bob.commonutil.model.candidateportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumePersonalInformationModel {
    private String name;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullNameAsPerAadhar;
    private String fullNameAsPerTenth;
    private String fullNameAsPerBirthCertificate;
    private String fatherName;
    private String motherName;
    private String spouseName;
    private String dob;
    private String gender;
    private String email;
    private String phone;
    private String mobile;
    private String alternatePhoneNumber;
    private String address;
    private String country;
    private String nationality;
    private String religion;
    private String category;
    private String caste;
    private String bloodType;
    private String maritalStatus;
    private String domicileState;
    private String countryOfBirth;
    private String stateOfBirth;
    private String townOfBirth;
    private String communitySegment;
    private String motherTongue;
    private String language1;
    private String language2;
    private String language3;
    private String differentlyAbled;
    private Double cibilScore;
    private List<String> socialMediaProfiles;
    private String githubLink;
    private String skills;
    private String currentEmployer;
    private String currentDesignation;
    private String totalExperience;
}
