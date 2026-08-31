package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.dto.JobPositionsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.CandidateOfferStatus;
import com.bob.db.enums.ReservationType;
import com.bob.db.mapper.CandidateMeritListMapper;
import com.bob.db.mapper.JobPositionsMapper;
import com.bob.db.repository.*;
import com.bob.db.model.MeritCandidateDTO;
import com.bob.jobportal.model.MeritListSummary;
import com.bob.jobportal.model.VacancyMatrixModel;
import com.bob.jobportal.util.ReservationCategoryCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidateSelectionService {

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private AgeRelaxationApplicationRepository ageRelaxationApplicationRepository;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private CandidateSelectionHelperService candidateSelectionHelperService;

    @Autowired
    private CandidateDisabilityDetailsRepository candidateDisabilityDetailsRepository;

    @Autowired
    private CandidateWrittenExamMarksRepository candidateWrittenExamMarksRepository;

    @Autowired
    private ReservationCategoryCache reservationCategoryCache;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private CandidateMeritListRepository candidateMeritListRepository;

    @Autowired
    private CandidateMeritListMapper candidateMeritListMapper;

    public List<MeritCandidateDTO> loadCandidateSelection(UUID positionId) {
        List<CandidateOfferStatus> stausList = List.of(CandidateOfferStatus.OFFER_AWAITED);
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findAllByJobPosition_IdAndStatusIn(positionId,stausList);
        List<UUID> applicationIds = candidateOffersEntities.stream().map(candidateOffersEntity -> candidateOffersEntity.getCandidateApplication().getId()).toList();
        List<CandidateApplicationsEntity> candidateApplications = candidateApplicationsRepository.findAllById(applicationIds);
        List<UUID> candidateIds = candidateApplications.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        List<CandidateProfileEntity> candidateProfileEntities = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        List<InterviewScheduleEntity> interviewScheduleEntities = interviewScheduleRepository.findAllByApplicationIdIn(applicationIds);
        Map<UUID,CandidateProfileEntity> candidateProfileEntityMap = candidateProfileEntities.stream().collect(Collectors.
                toMap(CandidateProfileEntity::getCandidateId, Function.identity()));
        Map<UUID,InterviewScheduleEntity> interviewScheduleMap = interviewScheduleEntities.stream().collect(Collectors.
                toMap(InterviewScheduleEntity::getApplicationId, Function.identity()));

        List<ReservationCategoriesEntity> reservationCategoriesEntities = reservationCategoriesRepository.findByReservationType(ReservationType.VERTICAL, Sort.by(Sort.Direction.ASC, AppConstants.MASTER_DISPLAY_ORDER));
        Map<UUID,String> reservationCategorieMap = reservationCategoriesEntities.stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId, ReservationCategoriesEntity::getCategoryName));
        List<CandidateDisabilityDetailsEntity> candidateDisabilityDetailsEntities = candidateDisabilityDetailsRepository.findAllByCandidateIdIn(candidateIds);

        Map<UUID, List<CandidateDisabilityDetailsEntity>> candidatePwdCategoryMap =candidateDisabilityDetailsEntities.stream()
                        .collect(Collectors.groupingBy(CandidateDisabilityDetailsEntity::getCandidateId));
        List<CandidateWrittenExamMarksEntity> candidateWrittenExamMarksEntities = candidateWrittenExamMarksRepository.findByPosition_Id(positionId);
        Map<UUID, CandidateWrittenExamMarksEntity> candidateWrittenExamMarksMap = candidateWrittenExamMarksEntities.stream()
                .collect(Collectors.toMap(entity -> entity.getApplication().getId(), Function.identity()));

        List<MeritCandidateDTO> meritCandidateDTOList = candidateApplications.stream().map(entity ->{
            UUID candidateId = entity.getCandidateId();
            UUID applicationId = entity.getId();
            UUID categoryId  = candidateProfileEntityMap.get(entity.getCandidateId()).getReservationCategoryId();
            CandidateProfileEntity candidateProfile = candidateProfileEntityMap.get(candidateId);
            return MeritCandidateDTO.builder()
                    .candidateId(candidateId)
                    .applicationId(applicationId)
                    .categoryId(categoryId)
                    .category(reservationCategorieMap.get(categoryId))
                    .offerStatus(CandidateOfferStatus.OFFER_AWAITED)
                    .dob(candidateProfile.getDateOfBirth())
                    .isPwd(candidateProfile.getDisability())
                    .pwdCategoryIds(candidatePwdCategoryMap.get(candidateId) == null ? List.of() : candidatePwdCategoryMap.get(candidateId).stream().map(CandidateDisabilityDetailsEntity::getDisabilityCategoryId).toList())
                    .isExServicemen(candidateProfile.getExServiceman() != null) // TODO: Check if this is correct, should it be candidateProfile.getExServiceman() != null
                    .interviewScore(interviewScheduleMap.get(entity.getId()) != null ? interviewScheduleMap.get(entity.getId()).getFinalScore() : BigDecimal.ZERO)
                    .writtenScore(candidateWrittenExamMarksMap.get(entity.getId()) != null ? candidateWrittenExamMarksMap.get(entity.getId()).getRankingMarksObtained() : BigDecimal.ZERO)
                    .build();
        }).toList();

        return meritCandidateDTOList;
    }

    public VacancyMatrixModel loadVacancyMatrix(JobPositionsDTO jobPosition) {
        return candidateSelectionHelperService.buildVacancyMatrixModel(jobPosition,reservationCategoryCache.getCategoryCodeMap());
    }

    public List<MeritCandidateDTO> loadOccupiedMeritCandidates(UUID positionId, List<CandidateOfferStatus> stausList) {
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findAllByJobPosition_IdAndStatusIn(positionId,stausList);
        List<UUID> applicationIds = candidateOffersEntities.stream().map(candidateOffersEntity -> candidateOffersEntity.getCandidateApplication().getId()).toList();
        List<CandidateMeritListEntity> candidateMeritListEntities = candidateMeritListRepository.findByApplicationIdIn(applicationIds);
        return candidateMeritListEntities.stream().map(candidateMeritListMapper::toMeritCandidateDTO).toList();
    }

    @Transactional
    public MeritListSummary loadMeritList(UUID positionId) {
        JobPositionsDTO jobPosition = jobPositionsMapper.toDto(positionsRepository.findById(positionId).orElseThrow(() -> new RuntimeException("Position not found")));
        VacancyMatrixModel vacancyMatrixModel = candidateSelectionHelperService.buildVacancyMatrixModel(jobPosition,reservationCategoryCache.getCategoryCodeMap());

        List<MeritCandidateDTO> occupiedCandidates = loadOccupiedMeritCandidates(positionId,List.of(CandidateOfferStatus.OFFER_ACCEPTED ,CandidateOfferStatus.OFFER_SENT,
                CandidateOfferStatus.L1_PENDING,CandidateOfferStatus.L2_PENDING,CandidateOfferStatus.L2_REJECTED,CandidateOfferStatus.L1_REJECTED,CandidateOfferStatus.OFFER_GENERATED));
        List<MeritCandidateDTO> rejectedCandidates = loadOccupiedMeritCandidates(positionId,List.of(CandidateOfferStatus.OFFER_REJECTED));
        candidateSelectionHelperService.occupyExistingSeats(vacancyMatrixModel,occupiedCandidates);
        candidateSelectionHelperService.addRejectedVacancies(vacancyMatrixModel,rejectedCandidates);


        List<MeritCandidateDTO> meritCandidateDTOList = new ArrayList<>(loadCandidateSelection(positionId));
        //meritCandidateDTOList.addAll(occupiedCandidates);
        //meritCandidateDTOList.addAll(rejectedCandidates);
        meritCandidateDTOList = candidateSelectionHelperService.calculateCombinedScores(meritCandidateDTOList, positionId);
        meritCandidateDTOList = candidateSelectionHelperService.loadLocationPreference(meritCandidateDTOList, jobPosition.getIsLocationWise(),positionId);
        meritCandidateDTOList = candidateSelectionHelperService.loadConcessionCandidates(meritCandidateDTOList, positionId);
        meritCandidateDTOList = candidateSelectionHelperService.determineGeneralEligibility(meritCandidateDTOList);
        meritCandidateDTOList= candidateSelectionHelperService.generateMasterMeritList(meritCandidateDTOList);
        meritCandidateDTOList = candidateSelectionHelperService.fillInitialGeneralSeats(meritCandidateDTOList, vacancyMatrixModel.getVacancyTracker(),reservationCategoryCache.getGeneralCategoryId());
        meritCandidateDTOList= candidateSelectionHelperService.applyPwdReservation(meritCandidateDTOList,vacancyMatrixModel.getVacancyTracker(),vacancyMatrixModel.getPwdTracker());
        meritCandidateDTOList = candidateSelectionHelperService.fillEWSSeats(meritCandidateDTOList, vacancyMatrixModel.getVacancyTracker(),reservationCategoryCache.getEwsCategoryId(),reservationCategoryCache.getGeneralCategoryId());
        meritCandidateDTOList = candidateSelectionHelperService.fillConvertedGeneralSeats(meritCandidateDTOList, vacancyMatrixModel.getVacancyTracker(),reservationCategoryCache.getGeneralCategoryId());
        meritCandidateDTOList= candidateSelectionHelperService.fillReservedSeats(meritCandidateDTOList,vacancyMatrixModel.getVacancyTracker());
        meritCandidateDTOList= candidateSelectionHelperService.generateWaitlist(meritCandidateDTOList,vacancyMatrixModel.getVacancyTracker());
        //meritCandidateDTOList= candidateSelectionHelperService.recalculateCategoryOrder(meritCandidateDTOList);

        List<UUID> applicationIds = meritCandidateDTOList.stream().map(MeritCandidateDTO::getApplicationId).toList();
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findAllByCandidateApplication_IdIn(applicationIds);

        Map<UUID,MeritCandidateDTO> meritCandidateDTOMap = meritCandidateDTOList.stream().collect(Collectors.toMap(MeritCandidateDTO::getApplicationId, Function.identity()));

        List<CandidateOffersEntity> updatedCandidateOffersEntities = new ArrayList<>();

        candidateOffersEntities.forEach(offer -> {
            MeritCandidateDTO candidateDTO = meritCandidateDTOMap.get(offer.getCandidateApplication().getId());
            if(candidateDTO != null) {
               offer.setSelectList(candidateDTO.getSelectionLabel());
               offer.setWaitList(candidateDTO.getWaitlistLabel());
               updatedCandidateOffersEntities.add(offer);
            }
        });


        candidateOffersRepository.saveAll(updatedCandidateOffersEntities);
        saveOrUpdateMeritList(positionId, meritCandidateDTOList);
        return MeritListSummary.builder()
                .vacancyMatrix(vacancyMatrixModel)
                .meritCandidates(meritCandidateDTOList)
                .build();
    }

    public void saveOrUpdateMeritList(
            UUID positionId,
            List<MeritCandidateDTO> meritCandidateDTOList
    ) {

        List<UUID> applicationIds =
                meritCandidateDTOList.stream()
                        .map(MeritCandidateDTO::getApplicationId)
                        .toList();

        List<CandidateMeritListEntity> existingEntities =
                candidateMeritListRepository
                        .findByApplicationIdIn(applicationIds);

        Map<UUID, CandidateMeritListEntity> existingMap =
                existingEntities.stream()
                        .collect(Collectors.toMap(
                                CandidateMeritListEntity::getApplicationId,
                                Function.identity()
                        ));

        List<CandidateMeritListEntity> entitiesToSave =
                new ArrayList<>();

        for (MeritCandidateDTO dto : meritCandidateDTOList) {

            CandidateMeritListEntity entity =
                    existingMap.get(
                            dto.getApplicationId()
                    );

            if (entity == null) {

                entity =
                        candidateMeritListMapper
                                .toMeritCandidateEntity(
                                        dto,
                                        positionId
                                );
            }
            else {

                updateExistingEntity(
                        entity,
                        dto
                );
            }

            entitiesToSave.add(entity);
        }

        candidateMeritListRepository
                .saveAll(entitiesToSave);
    }

    private void updateExistingEntity( CandidateMeritListEntity entity, MeritCandidateDTO dto) {
        entity.setStateId(dto.getStateId());
        entity.setCityId(dto.getCityId());
        entity.setSelected(dto.getSelected());
        entity.setWaitlisted(dto.getWaitlisted());
        entity.setSelectedCategoryId(dto.getSelectedCategoryId());
        entity.setGeneralEligible(dto.getGeneralEligible());

        entity.setSelectedAgainstPwd(dto.getSelectedAgainstPwd());
        entity.setSelectedPwdCategoryId(dto.getSelectedPwdCategoryId());
        entity.setIsExServicemen(dto.getIsExServicemen());

        entity.setOverallRank(dto.getOverallRank());
        entity.setCategoryRank(dto.getCategoryRank());
        entity.setInterviewScore(dto.getInterviewScore());
        entity.setWrittenScore(dto.getWrittenScore());
        entity.setIsEWSSeat(dto.getIsEWSSeat());

        entity.setCategoryId(dto.getCategoryId());
        entity.setDob(dto.getDob());
        entity.setIsPwd(dto.getIsPwd());

        entity.setCombinedScore(dto.getCombinedScore());

        entity.setInterviewWeightedScore(dto.getInterviewWeightedScore());

        entity.setExamWeightedScore(dto.getExamWeightedScore());
    }

    @Transactional
    public MeritListSummary calculateMeritList(UUID positionId) {
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findByJobPosition_Id(positionId);
                //stream().filter(offer -> offer.getCandidateApplication().getApplicationStatus() == CandidateApplicationStatus.OFFER_AWAITED).toList();

        if(candidateOffersEntities.isEmpty()) {
            throw new CommonException("No candidate offers found for the given position");
        }
        Map<UUID,CandidateOffersEntity> candidateApplicationsEntityMap = candidateOffersEntities.stream()
                .collect(Collectors.toMap(offer -> offer.getCandidateApplication().getId(), Function.identity()));

        List<UUID> applicationIds = candidateOffersEntities.stream().map(offer -> offer.getCandidateApplication().getId()).toList();
        List<UUID> candidateIds =candidateOffersEntities.stream().map(offer -> offer.getCandidateApplication().getCandidateId()).toList();
        List<CandidateMeritListEntity> candidateMeritListEntities = candidateMeritListRepository.findByApplicationIdIn(applicationIds);

        List<MeritCandidateDTO> meritCandidateDTOList = candidateMeritListEntities.stream().map(candidateMeritListMapper::toMeritCandidateDTO).toList();

        List<CandidateProfileEntity> candidateProfileEntities = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        List<InterviewScheduleEntity> interviewScheduleEntities = interviewScheduleRepository.findAllByApplicationIdIn(applicationIds);
        Map<UUID,CandidateProfileEntity> candidateProfileEntityMap = candidateProfileEntities.stream().collect(Collectors.
                toMap(CandidateProfileEntity::getCandidateId, Function.identity()));
        Map<UUID,InterviewScheduleEntity> interviewScheduleMap = interviewScheduleEntities.stream().collect(Collectors.
                toMap(InterviewScheduleEntity::getApplicationId, Function.identity()));

        List<CandidateWrittenExamMarksEntity> candidateWrittenExamMarksEntities = candidateWrittenExamMarksRepository.findByPosition_Id(positionId);
        Map<UUID, CandidateWrittenExamMarksEntity> candidateWrittenExamMarksMap = candidateWrittenExamMarksEntities.stream()
                .collect(Collectors.toMap(entity -> entity.getApplication().getId(), Function.identity()));


        meritCandidateDTOList = meritCandidateDTOList.stream()
                .map(candidateDTO -> {

                    CandidateOffersEntity offer =
                            candidateApplicationsEntityMap.get(candidateDTO.getApplicationId());

                    BigDecimal interviewScore =
                            interviewScheduleMap.get(candidateDTO.getApplicationId()) != null
                                    ? interviewScheduleMap.get(candidateDTO.getApplicationId()).getFinalScore()
                                    : BigDecimal.ZERO;

                    if (offer != null) {
                        candidateDTO.setSelectionLabel(offer.getSelectList());
                        candidateDTO.setWaitlistLabel(offer.getWaitList());
                    }

                    CandidateProfileEntity profile =
                            candidateProfileEntityMap.get(candidateDTO.getCandidateId());

                    if (profile != null) {
                        candidateDTO.setDob(profile.getDateOfBirth());
                        candidateDTO.setIsPwd(profile.getDisability());
                        candidateDTO.setIsExServicemen(profile.getExServiceman()!=null);
                    }

                    candidateDTO.setInterviewScore(interviewScore);

                    return candidateDTO; // REQUIRED
                })
                .toList();

        JobPositionsDTO jobPosition = jobPositionsMapper.toDto(positionsRepository.findById(positionId).orElseThrow(() -> new RuntimeException("Position not found")));
        VacancyMatrixModel vacancyMatrixModel = candidateSelectionHelperService.buildVacancyMatrixModel(jobPosition,reservationCategoryCache.getCategoryCodeMap());


        meritCandidateDTOList = candidateSelectionHelperService.calculateCombinedScores(meritCandidateDTOList, positionId);
        meritCandidateDTOList = candidateSelectionHelperService.loadConcessionCandidates(meritCandidateDTOList, positionId);
        meritCandidateDTOList = candidateSelectionHelperService.determineGeneralEligibility(meritCandidateDTOList);
        meritCandidateDTOList = candidateSelectionHelperService.generateMasterMeritList(meritCandidateDTOList);
        meritCandidateDTOList = candidateSelectionHelperService.fillInitialGeneralSeats(meritCandidateDTOList, vacancyMatrixModel.getVacancyTracker(),reservationCategoryCache.getGeneralCategoryId());
        meritCandidateDTOList = candidateSelectionHelperService.fillEWSSeats(meritCandidateDTOList, vacancyMatrixModel.getVacancyTracker(),reservationCategoryCache.getEwsCategoryId(),reservationCategoryCache.getGeneralCategoryId());
        meritCandidateDTOList = candidateSelectionHelperService.fillConvertedGeneralSeats(meritCandidateDTOList, vacancyMatrixModel.getVacancyTracker(),reservationCategoryCache.getGeneralCategoryId());
        meritCandidateDTOList = candidateSelectionHelperService.applyPwdReservation(meritCandidateDTOList,vacancyMatrixModel.getVacancyTracker(),vacancyMatrixModel.getPwdTracker());
        meritCandidateDTOList = candidateSelectionHelperService.fillReservedSeats(meritCandidateDTOList,vacancyMatrixModel.getVacancyTracker());
        meritCandidateDTOList = candidateSelectionHelperService.generateWaitlist(meritCandidateDTOList,vacancyMatrixModel.getVacancyTracker());
        meritCandidateDTOList=candidateSelectionHelperService.recalculateCategoryOrder(meritCandidateDTOList);

        Map<UUID,MeritCandidateDTO> meritCandidateDTOMap = meritCandidateDTOList.stream().collect(Collectors.toMap(MeritCandidateDTO::getApplicationId, Function.identity()));

        List<CandidateOffersEntity> updatedCandidateOffersEntities = new ArrayList<>();

        candidateOffersEntities.forEach(offer -> {
            MeritCandidateDTO candidateDTO = meritCandidateDTOMap.get(offer.getCandidateApplication().getId());
            if(candidateDTO != null) {
                offer.setSelectList(candidateDTO.getSelectionLabel());
                offer.setWaitList(candidateDTO.getWaitlistLabel());
                updatedCandidateOffersEntities.add(offer);
            }
        });

        List<CandidateMeritListEntity> updatedCandidateMeritListEntities = candidateMeritListEntities.stream().map(entity -> {
            MeritCandidateDTO candidateDTO = meritCandidateDTOMap.get(entity.getApplicationId());
            if(candidateDTO != null) {
                entity.setCombinedScore(candidateDTO.getCombinedScore());
                entity.setGeneralEligible(candidateDTO.getGeneralEligible());
                entity.setSelectedCategoryId(candidateDTO.getSelectedCategoryId());
            }
            return entity;
        }).toList();


        candidateOffersRepository.saveAll(updatedCandidateOffersEntities);
        candidateMeritListRepository.saveAll(updatedCandidateMeritListEntities);


        return MeritListSummary.builder()
                .vacancyMatrix(vacancyMatrixModel)
                .meritCandidates(meritCandidateDTOList)
                .build();
    }
}
