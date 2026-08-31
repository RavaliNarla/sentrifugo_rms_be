package com.bob.masterdata.Service;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.*;
import com.bob.db.entity.LanguageMasterEntity;
import com.bob.db.entity.ReservationCategoriesEntity;
import com.bob.db.enums.ReservationType;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import com.bob.masterdata.Model.GetCompleteDataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DisplayService {

    @Autowired
    private DepartmentsService departmentsService;

    @Autowired
    private CountryService countryService;

    @Autowired
    private StateService stateService;

    @Autowired
    private CityService cityService;


    @Autowired
    private SkillService skillService;

    @Autowired
    private JobGradeService jobGradeService;

    @Autowired
    private EducationalQualificationsService educationalQualificationsService;

    @Autowired
    private MasterPositionsService masterPositionsService;

    @Autowired
    private ReservationCategoriesService reservationCategoriesService;

    @Autowired
    private MasterDDService masterDDService;

    @Autowired
    private DistrictService districtService;

    @Autowired
    private DocumentTypesService documentTypesService;

    public GetCompleteDataResponse getAllData(){

        List<DepartmentsDTO> department = departmentsService.getAllDepartments();
        List<CountryDTO> countries=countryService.getAllCountries();
        List<StateDTO> states=stateService.getAllStates();
        List<CityDTO> cities=cityService.getAllCities();
        List<SkillDTO> skills= skillService.getAllSkills();
        List<JobGradeDTO> jobGrades=jobGradeService.getAllJobGrades();

        List<EducationQualificationsDTO> mandatory= educationalQualificationsService.getAllEduQual()
                .stream().sorted(Comparator.comparing(EducationQualificationsDTO::getQualificationName)).toList();

        List<EducationQualificationsDTO> preferred= educationalQualificationsService.getAllEduQual();
        List<MasterPositionsDTO> masterPositionsDTOS = masterPositionsService.getAll();
        List<ReservationCategoriesDTO> reservationCategories = reservationCategoriesService.getAllResCategories(ReservationType.VERTICAL);
        List<GenderMasterDTO> genderMasters= masterDDService.getAllGenders();
        List<MaritalStatusMasterDTO> maritalStatusMaster= masterDDService.getAllMaritalStatus();
        List<ReligionMasterDTO> religionMaster= masterDDService.getAllReligions();
        List<DisabilityCategoriesDTO> disabilityCategories= masterDDService.getAllDisabilities();
        List<UniversityMasterDTO> universityMaster = masterDDService.getAllUniversities();
        List<EducationTypeMasterDTO> educationTypeMaster = masterDDService.getAllEducationTypes();
        List<SpecializationMasterDTO> specializationMaster = masterDDService.getAllSpecializations()
                .stream().sorted(Comparator.comparing(SpecializationMasterDTO::getSpecializationName)).toList();;
        List<LanguageMasterDTO> languageMasters = masterDDService.getAllLanguages();
        List<PincodeDTO> pincodes = masterDDService.getAllPincodes();
        List<DistrictDTO> districts = districtService.getAllDistricts();
        List<DocumentTypesDTO> educationLevels  = documentTypesService.getDocumentTypesByType(AppConstants.EDUCATION_DOCS);
        List<EmployementTypesDTO> employementTypes = masterDDService.getAllEmployementTypes();

        return GetCompleteDataResponse.builder()
                .departments(department)
                .countries(countries)
                .states(states)
                .cities(cities)
                .skills(skills)
                .jobGrade(jobGrades)
                .mandatoryQualification(mandatory)
                .preferredQualification(preferred)
                .masterPositions(masterPositionsDTOS)
                .reservationCategories(reservationCategories)
                .genderMasters(genderMasters)
                .maritalStatusMaster(maritalStatusMaster)
                .religionMaster(religionMaster)
                .disabilityCategories(disabilityCategories)
                .educationLevels(educationLevels)
                .universityMaster(universityMaster)
                .educationTypeMaster(educationTypeMaster)
                .specializationMaster(specializationMaster)
                .languageMasters(languageMasters)
                .pincodes(pincodes)
                .districts(districts)
                .employementTypes(employementTypes)
                .build();
    }




}
