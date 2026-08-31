package com.bob.masterdata.Model;

import com.bob.db.dto.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetCompleteDataResponse {
    private List<DepartmentsDTO> departments;

    private List<CountryDTO> countries;

    private List<CityDTO> cities;

    private List<StateDTO> states;

    private List<SkillDTO> skills;

    private List<JobGradeDTO> jobGrade;

    private List<EducationQualificationsDTO> preferredQualification;

    private List<MasterPositionsDTO> masterPositions;

    private List<ReservationCategoriesDTO> reservationCategories;

    private List<GenderMasterDTO> genderMasters;

    private List<MaritalStatusMasterDTO> maritalStatusMaster;
    private List<ReligionMasterDTO> religionMaster;
    private List<DisabilityCategoriesDTO> disabilityCategories;

    private List<LanguageMasterDTO> languageMasters;

    private List<PincodeDTO> pincodes;
    private List<DistrictDTO> districts;

    private List<DocumentTypesDTO> educationLevels;
    private List<SpecializationMasterDTO> specializationMaster;
    private List<EducationQualificationsDTO> mandatoryQualification;
    private List<EducationTypeMasterDTO> educationTypeMaster;
    private List<UniversityMasterDTO> universityMaster;
    private List<EmployementTypesDTO> employementTypes;


}
