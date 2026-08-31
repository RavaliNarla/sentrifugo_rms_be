package com.bob.candidateportal.model;

import com.bob.db.dto.EducationDTO;
import com.bob.db.dto.WorkExperienceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicantModel {
    private String requisitionName;
    private String requisitionCode;
    private String positionName;
    private String applicationNo;
    private String fullName;
    private String currentAddressLine1;
    private String currentAddressLine2;
    private String currentStateName;
    private String currentDistrictName ;
    private String currentCityName;
    private String currentPincode;
    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentStateName;
    private String permanentDistrictName;
    private String permanentCityName;
    private String permanentPincode;
    private String mobile;
    private String email;
    private String motherName;
    private String fatherName;
    private String gender;
    private String religion;
    private String category;
    private String caste;
    private String dateOfBirth;
    private String age;
    private String exServicemen;
    private String physicalDisability;
    private String examCenter;
    private String nationality;
    private String maritalStatus;
    private String spouseName;
    private String twinSibling;
    private String twinSiblingName;
    private String cibilScore;
    private String currentCTC;
    private String expectedCTC;
    private String socialMediaLink;
    private String statePreference1;
    private String statePreference2;
    private String statePreference3;
    private String locationPreference1;
    private String locationPreference2;
    private String locationPreference3;
    private String govtEmployment; //Central govt
    private String lowerPost; //Lower post advertised
    private String riotsFamily;//1984 riots
    private String religiousMinority; //Minority
    private String publicSector;
    private String disciplinaryAction;
    private String disciplinaryDetails;

    // Photo and Signature
    private String photoUrl;
    private String signatureUrl;

    //language proficiency
    private String languageProficiency;
    private String localLanguage;
    private String isLocalLanguageStudied;


    // Education and Experience Lists
    private List<EducationModel> educationList;
    private List<WorkExperienceModel> experienceList;
    private Map<String,String> additionalFieldsMap;

}
