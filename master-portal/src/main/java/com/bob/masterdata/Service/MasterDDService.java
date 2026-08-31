package com.bob.masterdata.Service;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.*;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MasterDDService {

    @Autowired
    private GenderMasterRepository genderMasterRepository;
    @Autowired
    private GenderMasterMapper genderMasterMapper;

    @Autowired
    private MaritalStatusMasterMapper maritalStatusMasterMapper;

    @Autowired
    private MaritalStatusMasterRepository maritalStatusMasterRepository;

    @Autowired
    private ReligionMasterRepository religionMasterRepository;

    @Autowired
    private ReligionMasterMapper religionMasterMapper;

    @Autowired
    private DisabilityCategoriesRepository disabilityCategoriesRepository;

    @Autowired
    private DisabilityCategoriesMapper disabilityCategoriesMapper;

    @Autowired
    private UniversityMasterMapper universityMasterMapper;

    @Autowired
    private UniversityMasterRepository universityMasterRepository;

    @Autowired
    private EducationTypeMasterMapper educationTypeMasterMapper;

    @Autowired
    private SpecializationMasterMapper specializationMasterMapper;

    @Autowired
    private SpecializationMasterRepository specializationMasterRepository;

    @Autowired
    private EducationTypeMasterRepository educationTypeMasterRepository;

    @Autowired
    private LanguageMasterMapper languageMasterMapper;

    @Autowired
    private LanguageMasterRepository languageMasterRepository;

    @Autowired
    private PincodeMapper pincodeMapper;

    @Autowired
    private PincodeRepository pincodeRepository;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private InterviewCommitteeMapper interviewCommitteeMapper;

    @Autowired
    private RequestTypesRepository requestTypesRepository;

    @Autowired
    private RequestTypesMapper requestTypesMapper;

    @Autowired
    private EmployementTypesRepository employementTypesRepository;

    @Autowired
    private EmployementTypesMapper employementTypesMapper;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private StateLanguagesRepository stateLanguagesRepository;

    @Autowired
    private StateLanguagesMapper stateLanguagesMapper;

    @Autowired
    private MedicalCentresRepository medicalCentresRepository;

    @Autowired
    private MedicalCentresMapper medicalCentresMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ScoringWeightageMapper scoringWeightageMapper;

    @Autowired
    private ScoringWeightageRepository scoringWeightageRepository;

    @Autowired
    private ExclusionsMasterRepository exclusionsMasterRepository;

    @Autowired
    private ExclusionsMasterMapper exclusionsMasterMapper;

    @Autowired
    private QualificationGroupMappingRepository qualificationGroupMappingRepository;

    @Autowired
    private QualificationGroupMappingMapper qualificationGroupMappingMapper;

    @Autowired
    private EducationGroupsRepository educationGroupsRepository;

    @Autowired
    private EducationGroupsMapper educationGroupsMapper;

    @Autowired
    private ExservicemanCategoryMapper exservicemanCategoryMapper;

    @Autowired
    private ExservicemanCategoryRepository exservicemanCategoryRepository;

    public List<GenderMasterDTO> getAllGenders() {
        return genderMasterMapper.toDtoList(genderMasterRepository.findAll());
    }

    public List<MaritalStatusMasterDTO> getAllMaritalStatus() {
        return maritalStatusMasterMapper.toDtoList(maritalStatusMasterRepository.findAll());
    }

    public List<ReligionMasterDTO> getAllReligions(){
        return religionMasterMapper.toDTOList(religionMasterRepository.findAll());
    }


    public List<DisabilityCategoriesDTO> getAllDisabilities() {
        return disabilityCategoriesMapper.toDtoList(disabilityCategoriesRepository.findAll(Sort.by(Sort.Direction.ASC,
                AppConstants.MASTER_DISPLAY_ORDER)));
    }


    public List<UniversityMasterDTO> getAllUniversities() {
        return universityMasterMapper.toDTOList(universityMasterRepository.findAll());
    }

    public List<EducationTypeMasterDTO> getAllEducationTypes() {
        return educationTypeMasterMapper.toDTOList(educationTypeMasterRepository.findAll());
    }

    public List<SpecializationMasterDTO> getAllSpecializations() {
        return specializationMasterMapper.toDTOList(specializationMasterRepository.findAll());
    }

    public List<LanguageMasterDTO> getAllLanguages() {
        return languageMasterMapper.toDtoList(languageMasterRepository.findAll());
    }

    public List<PincodeDTO> getAllPincodes() {
        return pincodeMapper.toDTOList(pincodeRepository.findAll());
    }

    public List<InterviewCommitteeDTO> getAllInterviewCommittees() {
        return interviewCommitteeMapper.toDTOList(interviewCommitteeRepository.findAll());
    }

    public List<RequestTypesDTO> getAllRequestTypes() {
        return requestTypesMapper.toDTOList(requestTypesRepository.findAll());
    }

    public List<EmployementTypesDTO> getAllEmployementTypes() {
        return employementTypesMapper.toDtoList(employementTypesRepository.findAll());
    }

    public List<InterviewCentresDTO> getAllInterviewCentres() {
        return interviewCentresMapper.toDtoList(interviewCentresRepository.findAll());
    }

    public List<StateLanguagesDTO> getStateLanguages() {
        return stateLanguagesMapper.toDtoList(stateLanguagesRepository.findAll());
    }

    public List<MedicalCentresDTO> getAllMedicalCentres() {
        return medicalCentresMapper.toDtoList(medicalCentresRepository.findAll());
    }
    public List<RoleDTO> getAllRoles(){
        return roleMapper.toDtoList(roleRepository.findAll());
    }

    public ScoringWeightageDTO getAllWeightageScores() {
        return scoringWeightageMapper.toDto(scoringWeightageRepository.findAll().get(0));
    }

    public List<ExclusionsMasterDTO> getAllExclusions() {
        return exclusionsMasterMapper.toDtoList(exclusionsMasterRepository.findAll());
    }
    public List<QualificationGroupMappingDTO> getAllQualificationGrouping() {
        return qualificationGroupMappingMapper.toDTOList(qualificationGroupMappingRepository.findAll());
    }

    public List<EducationGroupsDTO> getAllEducationGroups() {
        return educationGroupsMapper.toDTOList(educationGroupsRepository.findAll());
    }

    public List<ExservicemanCategoryDTO> getAllExServiceMen() {
        return exservicemanCategoryMapper.toDtoList(exservicemanCategoryRepository.findAll());
    }
}
