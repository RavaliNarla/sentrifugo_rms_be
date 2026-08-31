package com.bob.commonutil.util;

import com.bob.db.entity.*;
import com.bob.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CandidateLookupUtil {

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private StateRepository statesRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobRequisitionsRepository requisitionsRepository;


    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private ZonalStatesRepository zonalStatesRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private JobGradeRepository jobGradeRepository;

    @Autowired
    private EmployementTypesRepository employementTypesRepository;

    @Autowired
    private TemplatesRepository templatesRepository;

    @Autowired
    private UserSignatryRepository userSignatryRepository;

    public Map<UUID, CandidateProfileEntity> buildCandidateProfileMap(List<CandidateOffersEntity> entities) {
        List<UUID> candidateIds = entities.stream()
                .map(CandidateOffersEntity::getCandidate)
                .filter(Objects::nonNull)
                .map(CandidatesEntity::getId)
                .distinct()
                .collect(Collectors.toList());
        return candidateProfileRepository.findAllByCandidateIdIn(candidateIds)
                .stream()
                .collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, p -> p));
    }

    public Map<UUID, MasterPositionsEntity> buildMasterPositionMap(List<CandidateOffersEntity> entities) {
        List<UUID> masterPositionIds = entities.stream()
                .map(CandidateOffersEntity::getDesignation)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return masterPositionsRepository.findAllById(masterPositionIds)
                .stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, m -> m));
    }

    public Map<UUID, ReservationCategoriesEntity> buildReservationCategoryMap(Map<UUID, CandidateProfileEntity> candidateProfileMap,Map<UUID,CandidateMeritListEntity> meritListEntityMap) {
        List<UUID> reservationCategoryIds = candidateProfileMap.values().stream()
                .map(CandidateProfileEntity::getReservationCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> selectedCategoryIds = meritListEntityMap!= null ? meritListEntityMap.values().stream()
                .map(CandidateMeritListEntity::getSelectedCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList() : new ArrayList<>();

        Set<UUID> allReservationCategoryIds = new HashSet<>();
        allReservationCategoryIds.addAll(reservationCategoryIds);
        allReservationCategoryIds.addAll(selectedCategoryIds);
        return reservationCategoriesRepository.findAllById(allReservationCategoryIds)
                .stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId, r -> r));
    }

    public Map<UUID, CandidateAddressEntity> buildCandidateAddressMap(List<CandidateOffersEntity> entities){
        List<UUID> candidateIds = entities.stream()
                .map(CandidateOffersEntity::getCandidate)
                .filter(Objects::nonNull)
                .map(CandidatesEntity::getId)
                .distinct()
                .collect(Collectors.toList());
        return candidateAddressRepository.findAllByCandidateIdIn(candidateIds)
                .stream().collect(
                        Collectors.toMap(
                                CandidateAddressEntity::getCandidateId,
                                Function.identity()
                        )
                );

    }

    public Map<UUID,StateEntity> buildCandidateStateMap(Map<UUID,CandidateAddressEntity> candidateAddressMap){
        List<UUID> candidateStateIds = candidateAddressMap.values().stream().map(
                CandidateAddressEntity::getStateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return statesRepository.findAllById(candidateStateIds)
                .stream().collect(
                        Collectors.toMap(
                                StateEntity::getId,
                                Function.identity()
                        )
                );
    }

    public Map<UUID,DistrictEntity> buildCandidateDistrictMap(Map<UUID,CandidateAddressEntity> candidateAddressMap){
        List<UUID> candidateStateIds = candidateAddressMap.values().stream().map(
                        CandidateAddressEntity::getDistrictId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return districtRepository.findAllById(candidateStateIds)
                .stream().collect(
                        Collectors.toMap(
                                DistrictEntity::getId,
                                Function.identity()
                        )
                );
    }

    public Map<UUID,JobPositionsEntity> buildJobPositionMap(List<CandidateOffersEntity> entities){
        List<UUID> positionsIds = entities.stream()
                .map((entity)->entity.getCandidateApplication().getPositionId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return positionsRepository.findAllById(positionsIds)
                .stream()
                .collect(Collectors.toMap(
                        JobPositionsEntity::getId,
                        Function.identity()
                ));

    }



    public Map<UUID,JobRequisitionsEntity> buildJobRequistionMap(Map<UUID,JobPositionsEntity> jobPositionsMap){
        List<UUID> requisitionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getRequisitionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return requisitionsRepository.findAllById(requisitionIds)
                .stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        Function.identity()
                ));

    }

    public Map<UUID,DepartmentsEntity> buildDepartmentsMap(Map<UUID,JobPositionsEntity> jobPositionsMap){
        List<UUID> deptIds = jobPositionsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .map(JobPositionsEntity::getDeptId)
                .distinct()
                .toList();
        return departmentsRepository.findAllById(deptIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                DepartmentsEntity::getId,
                                Function.identity()
                        )
                );

    }

    public Map<UUID,JobGradeEntity> buildJobGradeMap(Map<UUID,JobPositionsEntity> jobPositionsMap){
        List<UUID> jobGradeIds = jobPositionsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .map(JobPositionsEntity::getGradeId)
                .distinct()
                .toList();
        return jobGradeRepository.findAllById(jobGradeIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                JobGradeEntity::getId,
                                Function.identity()
                        )
                );

    }


    public Map<UUID,EmployementTypesEntity> buildJobEmpTypeMap(Map<UUID,JobPositionsEntity> jobPositionsMap){
        List<UUID>  empTypeIds = jobPositionsMap.values()
                .stream()
                .filter(Objects::nonNull)
                .map(JobPositionsEntity::getEmploymentType)
                .distinct()
                .toList();
        return employementTypesRepository.findAllById(empTypeIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                EmployementTypesEntity::getId,
                                Function.identity()
                        )
                );

    }

    public Map<UUID,InterviewCentresEntity> buildMedicalCentreMap(List<CandidateOffersEntity> entities){
        List<UUID> medicalCentreIds = entities.stream()
                .map(CandidateOffersEntity::getMedicalCenterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return interviewCentresRepository.findAllById(medicalCentreIds)
                .stream()
                .collect(Collectors.toMap(
                        InterviewCentresEntity::getId,
                        Function.identity()
                ));

    }


    public Map<UUID,ZonalStatesEntity> buildZonalStateMap(Map<UUID,InterviewCentresEntity> medicalCentreMap){
        List<UUID> medicalStateIds = medicalCentreMap.values()
                .stream()
                .map(InterviewCentresEntity::getZonalStateId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<UUID> allZonalStateIds = new HashSet<>(medicalStateIds);
        allZonalStateIds.addAll(medicalStateIds);

        return zonalStatesRepository.findAllById(allZonalStateIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                ZonalStatesEntity::getId,
                                Function.identity()
                        )
                );
    }

    public Map<UUID,TemplatesEntity> buildTemplatesMap(List<CandidateOffersEntity> entities){
        Set<UUID> templateIds = entities.stream()
                .map(CandidateOffersEntity::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return templatesRepository.findAllById(templateIds).stream()
                .collect(Collectors.toMap(
                        TemplatesEntity::getId,
                        Function.identity()
                ));
    }

    public Map<UUID,UserSignatryEntity> buildSignatryMap(List<CandidateOffersEntity> entities){
        Set<UUID> signatryIds = entities.stream()
                .map(CandidateOffersEntity::getSignatryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userSignatryRepository.findAllById(signatryIds).stream()
                .collect(Collectors.toMap(
                        UserSignatryEntity::getId,
                        Function.identity()
                ));
    }
}
