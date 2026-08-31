package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.*;
import com.bob.commonutil.util.*;
import com.bob.db.dto.CandidateOffersDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.CandidateOffersMapper;
import com.bob.db.repository.*;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.db.util.excel.ExcelHeader;
import com.bob.jobportal.model.CandidateOfferExcelModel;
import com.bob.jobportal.model.CandidateOfferRequestModel;
import com.bob.jobportal.model.MeritListDownloadModel;
import com.bob.jobportal.model.SendOfferRequestModel;
import com.bob.jobportal.util.OfferLetterGenerationUtil;
import com.bob.jobportal.util.OfferMailModel;
import jakarta.servlet.ServletOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.thymeleaf.TemplateEngine;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@lombok.extern.slf4j.Slf4j
public class CandidateOfferService {

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private VacancyStatisticsService vacancyStatisticsService;

    @Autowired
    private CandidateOffersMapper candidateOffersMapper;

    @Autowired
    private CandidateLookupUtil candidateLookupUtil;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private PositionsRepository jobPositionsRepository;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private CommonMailService commonMailService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private TemplatesRepository templatesRepository;

    @Autowired
    private FileService fs;

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;

    @Autowired
    private CandidateDisabilityDetailsRepository candidateDisabilityDetailsRepository;

    @Autowired
    private DisabilityCategoriesRepository disabilityCategoriesRepository;

    @Autowired
    private WrittenExamConfigurationRepository writtenExamConfigurationRepository;

    @Autowired
    private CandidateWrittenExamMarksRepository writtenExamMarksRepository;

    @Autowired
    private CandidateConcessionsRepository candidateConcessionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private CandidateMeritListRepository candidateMeritListRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private CandidateWrittenExamMarksRepository candidateWrittenExamMarksRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private OfferLetterGenerationUtil offerLetterGenerationUtil;

    @Value("${candidate.offer.upload.path}")
    private String offerUploadPath;


    @Autowired
    private OfferApprovalRepository offerApprovalRepository;

    @Autowired
    private OfferApprovalHistoryRepository offerApprovalHistoryRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public void sendToOfferPool(List<UUID> interviewIds) {
        // Fetch all InterviewScheduleEntity in a single DB call
        List<InterviewScheduleEntity> interviewScheduleEntityList = interviewScheduleRepository.findAllById(interviewIds);

        List<UUID> applicationIds = interviewScheduleEntityList.stream()
                .map(InterviewScheduleEntity::getApplicationId)
                .collect(Collectors.toList());

        // Fetch all CandidateApplicationsEntity for the given applicationIds into a map for efficient lookup
        Map<UUID, CandidateApplicationsEntity> applicationMap = candidateApplicationsRepository.findAllById(applicationIds)
                .stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, app -> app));

        // Batch fetch JobPositionsEntity to resolve masterPositionId upfront
        List<UUID> positionIds = applicationMap.values().stream()
                .map(CandidateApplicationsEntity::getPositionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, UUID> positionToMasterPositionMap = jobPositionsRepository.findAllById(positionIds)
                .stream()
                .filter(jp -> jp.getMasterPositionId() != null)
                .collect(Collectors.toMap(JobPositionsEntity::getId, JobPositionsEntity::getMasterPositionId));

        List<CandidateApplicationsEntity> updatedApplications = new ArrayList<>();
        List<CandidateOffersEntity> candidateOffers = new ArrayList<>();

        // Create old schedule copies for audit trail
        List<InterviewScheduleEntity> oldSchedules = new ArrayList<>();
        for (InterviewScheduleEntity schedule : interviewScheduleEntityList) {
            InterviewScheduleEntity oldSchedule = InterviewScheduleEntity.builder()
                    .applicationId(schedule.getApplicationId())
                    .candidateId(schedule.getCandidateId())
                    .panelId(schedule.getPanelId())
                    .interviewStartAt(schedule.getInterviewStartAt())
                    .interviewEndAt(schedule.getInterviewEndAt())
                    .interviewDurationMinutes(schedule.getInterviewDurationMinutes())
                    .meetingLink(schedule.getMeetingLink())
                    .zonalOfficeId(schedule.getZonalOfficeId())
                    .finalScore(schedule.getFinalScore())
                    .interviewStatus(schedule.getInterviewStatus())
                    .zonalVerificationStatus(schedule.getZonalVerificationStatus())
                    .zonalSubmitBeforeDate(schedule.getZonalSubmitBeforeDate())
                    .zonalHrComments(schedule.getZonalHrComments())
                    .lptRequired(schedule.getLptRequired())
                    .lptStatus(schedule.getLptStatus())
                    .mailSendStatus(schedule.getMailSendStatus())
                    .mailSentOn(schedule.getMailSentOn())
                    .mailComments(schedule.getMailComments())
                    .build();
            oldSchedule.setId(schedule.getId());
            oldSchedules.add(oldSchedule);
        }

        for (InterviewScheduleEntity interviewScheduleEntity : interviewScheduleEntityList) {
            UUID applicationId = interviewScheduleEntity.getApplicationId();
            CandidateApplicationsEntity application = applicationMap.get(applicationId);

            if (application != null) {
                // Update the status of CandidateApplications to OFFER_AWAITED
                application.setApplicationStatus(CandidateApplicationStatus.OFFER_AWAITED);
                updatedApplications.add(application);

                // Update InterviewScheduleEntity status to OFFER_AWAITED
                interviewScheduleEntity.setInterviewStatus(InterviewSchedulingStatus.OFFER_AWAITED);

                // Create CandidateOffersEntity, associating it with the fetched and updated application entity
                // designation stores masterPositionId directly for efficient reads without joining through jobPosition
                candidateOffers.add(CandidateOffersEntity.builder()
                        .interviewSchedule(InterviewScheduleEntity.builder().id(interviewScheduleEntity.getId()).build())
                        .candidateApplication(application)
                        .status(CandidateOfferStatus.OFFER_AWAITED)
                        .jobPosition(JobPositionsEntity.builder().id(application.getPositionId()).build())
                        .candidate(CandidatesEntity.builder().id(application.getCandidateId()).build())
//                        .interviewCenter(InterviewCentresEntity.builder().id(interviewScheduleEntity.getZonalOfficeId()).build()) //TODO:need to change later
                        .designation(positionToMasterPositionMap.get(application.getPositionId()))
                        .build());
            }
        }

        List<CandidateCompensationEntity> candidateCompensationEntities = candidateCompensationRepository.findByInterviewScheduleIdIn(interviewIds);
        if (!candidateCompensationEntities.isEmpty()) {
            for (CandidateCompensationEntity candidateCompensationEntity : candidateCompensationEntities) {
                candidateCompensationEntity.setCompensationStatus(CompensationStatus.COMPLETED);
            }
            candidateCompensationRepository.saveAll(candidateCompensationEntities);
        }
        candidateMeritListRepository.saveAll(saveMeritListData(updatedApplications));
        // Save all updated CandidateApplications, InterviewSchedules, and new CandidateOffers in batch
        candidateApplicationsRepository.saveAllWithWorkflow(updatedApplications);
        interviewScheduleRepository.saveAllWithAudit(oldSchedules, interviewScheduleEntityList);
        candidateOffersRepository.saveAll(candidateOffers);
    }

    public List<CandidateMeritListEntity> saveMeritListData(List<CandidateApplicationsEntity> candidateApplications) {
        List<UUID> candidateIds = candidateApplications.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        List<UUID> applicationIds = candidateApplications.stream().map(CandidateApplicationsEntity::getId).toList();
        List<UUID> positionIds = candidateApplications.stream().map(CandidateApplicationsEntity::getPositionId).distinct().toList();
        List<CandidateProfileEntity> candidateProfileEntities = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        List<InterviewScheduleEntity> interviewScheduleEntities = interviewScheduleRepository.findAllByApplicationIdIn(applicationIds);
        Map<UUID, CandidateProfileEntity> candidateProfileEntityMap = candidateProfileEntities.stream().collect(Collectors.
                toMap(CandidateProfileEntity::getCandidateId, Function.identity()));
        Map<UUID, InterviewScheduleEntity> interviewScheduleMap = interviewScheduleEntities.stream().collect(Collectors.
                toMap(InterviewScheduleEntity::getApplicationId, Function.identity()));

        List<ReservationCategoriesEntity> reservationCategoriesEntities = reservationCategoriesRepository.findByReservationType(ReservationType.VERTICAL, Sort.by(Sort.Direction.ASC, AppConstants.MASTER_DISPLAY_ORDER));
        Map<UUID, String> reservationCategorieMap = reservationCategoriesEntities.stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId, ReservationCategoriesEntity::getCategoryName));
        List<CandidateDisabilityDetailsEntity> candidateDisabilityDetailsEntities = candidateDisabilityDetailsRepository.findAllByCandidateIdIn(candidateIds);

        Map<UUID, List<CandidateDisabilityDetailsEntity>> candidatePwdCategoryMap = candidateDisabilityDetailsEntities.stream()
                .collect(Collectors.groupingBy(CandidateDisabilityDetailsEntity::getCandidateId));
        List<CandidateWrittenExamMarksEntity> candidateWrittenExamMarksEntities = candidateWrittenExamMarksRepository.findByPosition_IdIn(positionIds);
        Map<UUID, CandidateWrittenExamMarksEntity> candidateWrittenExamMarksMap = candidateWrittenExamMarksEntities.stream()
                .collect(Collectors.toMap(entity -> entity.getApplication().getId(), Function.identity()));


        return candidateApplications.stream().map(application -> {
            CandidateProfileEntity profile = candidateProfileEntityMap.get(application.getCandidateId());
            InterviewScheduleEntity schedule = interviewScheduleMap.get(application.getId());
            String categoryName = reservationCategorieMap.get(profile.getReservationCategoryId());
            List<CandidateDisabilityDetailsEntity> disabilityDetails = candidatePwdCategoryMap.get(application.getCandidateId());
            CandidateWrittenExamMarksEntity writtenExamMarks = candidateWrittenExamMarksMap.get(application.getId());

            return CandidateMeritListEntity.builder()
                    .applicationId(application.getId())
                    .candidateId(application.getCandidateId())
                    .positionId(application.getPositionId())
                    .categoryId(profile.getReservationCategoryId())
                    .isPwd(profile.getDisability())
                    .dob(profile.getDateOfBirth())
                    .applicationNo(application.getApplicationNo())
                    .build();
        }).toList();

    }

    @Transactional
    public List<CandidateOfferRequestModel> getOffersByPositionId(UUID positionId) {
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findByJobPosition_Id(positionId);
        List<CandidateOfferRequestModel> resList = new ArrayList<>();
        List<UUID> applicationIds=candidateOffersEntities.stream().map(ca->ca.getCandidateApplication().getId()).toList();
        if (candidateOffersEntities.isEmpty()) {
            return resList;
        }

        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateLookupUtil.buildCandidateProfileMap(candidateOffersEntities);
        Map<UUID, MasterPositionsEntity> masterPositionMap = candidateLookupUtil.buildMasterPositionMap(candidateOffersEntities);
        Map<UUID, ReservationCategoriesEntity> reservationCategoryMap = candidateLookupUtil.buildReservationCategoryMap(candidateProfileMap,null);
        Map<UUID,CandidateMeritListEntity> candidateMeritListEntityMap = candidateMeritListRepository.findByPositionId(positionId).stream()
                .collect(Collectors.toMap(
                        CandidateMeritListEntity::getApplicationId,
                        Function.identity()
                ));
        List<UUID> stateIds = candidateMeritListEntityMap.values().stream().map(CandidateMeritListEntity::getStateId).filter(Objects::nonNull).toList();
        List<UUID> cityIds = candidateMeritListEntityMap.values().stream().map(CandidateMeritListEntity::getCityId).filter(Objects::nonNull).toList();
        Map<UUID,String> stateNameMap = stateRepository.findAllById(stateIds).stream()
                .collect(Collectors.toMap(
                        StateEntity::getId,
                        StateEntity::getStateName
                ));
        Map<UUID,String> cityNameMap = cityRepository.findAllById(cityIds).stream()
                .collect(Collectors.toMap(
                        CityEntity::getId,
                        CityEntity::getCityName
                ));
        List<UUID> offerIds=candidateOffersEntities.stream().map(CandidateOffersEntity::getId).collect(Collectors.toSet()).stream().toList();
        Map<UUID, UUID> offersEntityMap = offerApprovalRepository.findByOfferIdIn(offerIds)
                .stream()
                .collect(Collectors.toMap(
                        OfferApprovalsEntity::getOfferId,
                        OfferApprovalsEntity::getId
                ));
        LocalDate cutOffDate=null;
        Optional<JobPositionsEntity> jobPositionOpt=jobPositionsRepository.findById(positionId);
        if(jobPositionOpt.isPresent()){
            Optional<JobRequisitionsEntity> jobRequisitionsEntityOpt=jobRequisitionsRepository.findById(jobPositionOpt.get().getRequisitionId());
            if(jobRequisitionsEntityOpt.isPresent()){
                cutOffDate=jobRequisitionsEntityOpt.get().getCutoffDate();
            }
        }
        Map<UUID,CandidateConcessionsEntity> candidateConcessionsEntityMap=candidateConcessionsRepository.findAllByApplicationIdIn(applicationIds)
                .stream().collect(Collectors.toMap(CandidateConcessionsEntity::getApplicationId,c->c));
        for (CandidateOffersEntity entity : candidateOffersEntities) {
            CandidateProfileEntity candidateProfile = entity.getCandidate() != null ? candidateProfileMap.get(entity.getCandidate().getId()) : null;
            String reservationCategoryCode = offerLetterGenerationUtil.resolveReservationCategoryCode(candidateProfile, reservationCategoryMap);
            String designationName = offerLetterGenerationUtil.resolveDesignationName(entity.getDesignation(), masterPositionMap);
            CandidateOffersDTO offerDTO = candidateOffersMapper.toDTO(entity);
            offerDTO.setQualified(entity.getSelectList()!=null && !entity.getSelectList().isBlank());

            CandidateMeritListEntity meritListEntity = candidateMeritListEntityMap.get(entity.getCandidateApplication().getId());

            UUID cityId = meritListEntity != null ? meritListEntity.getCityId() : null;
            UUID stateId = meritListEntity != null ? meritListEntity.getStateId() : null;

            String location = cityNameMap.get(cityId);
            String state = stateNameMap.get(stateId);
            BigDecimal finalScore = meritListEntity != null ? meritListEntity.getCombinedScore() : null;
            LocalDate dob=meritListEntity!=null ?meritListEntity.getDob():null;
            String age=null;
            if (cutOffDate != null) {
                Period period = Period.between(dob, cutOffDate.plusDays(AppConstants.ONE));
                age = period.getYears() + " " + AppConstants.YEARS + " " + period.getMonths() + " " + AppConstants.MONTHS + " " + period.getDays() + " " + AppConstants.DAYS;
            }
            boolean ageConcession=candidateConcessionsEntityMap.get(entity.getCandidateApplication().getId()).getAgeConcession();
            BigDecimal writtenExamMarks=meritListEntity != null ? meritListEntity.getWrittenScore() : null;
            BigDecimal interviewExamMarks=meritListEntity != null ? meritListEntity.getInterviewScore() : null;
            CandidateOfferRequestModel resModel = CandidateOfferRequestModel.builder()
                    .candidateOffersDTO(offerDTO)
                    .candidateFullName(candidateProfile != null ? commonUtilityProvider.buildFullName(candidateProfile) : null)
                    .regNo(entity.getCandidateApplication() != null ? entity.getCandidateApplication().getApplicationNo() : null)
                    .designationName(designationName)
                    .reservationCategory(reservationCategoryCode)
                    .location(location)
                    .state(state)
                    .finalScore(finalScore)
                    .interviewSchedulingStatus(entity.getInterviewSchedule().getInterviewStatus())
                    .offerApprovalId(offersEntityMap.get(entity.getId()))
                    .candidateDob(dob.toString())
                    .age(age)
                    .hasAgeConcession(ageConcession)
                    .examMarks(writtenExamMarks)
                    .interviewMarks(interviewExamMarks)
                    .build();

            resList.add(resModel);
        }
        resList.sort(
                Comparator.comparing(
                                CandidateOfferRequestModel::getReservationCategory,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                        .thenComparing(
                                CandidateOfferRequestModel::getFinalScore,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
        );

        return resList;
    }

    public byte[] downloadOffersExcel(List<UUID> offerIds) throws IOException {
        List<CandidateOffersEntity> entities = candidateOffersRepository.findAllById(offerIds);

        List<CandidateOfferExcelModel> modelList = buildOfferExcelModels(entities);

        byte[] rawBytes = excelTemplateService.generateExcelTemplateWithData(CandidateOfferExcelModel.class, modelList, null).getFileContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(rawBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    if (AppConstants.OFFER_ID_HEADER.equals(cell.getStringCellValue())) {
                        sheet.setColumnHidden(cell.getColumnIndex(), true);
                        break;
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private List<CandidateOfferExcelModel> buildOfferExcelModels(List<CandidateOffersEntity> entities) {
        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateLookupUtil.buildCandidateProfileMap(entities);
        Map<UUID, MasterPositionsEntity> masterPositionMap = candidateLookupUtil.buildMasterPositionMap(entities);
        Map<UUID, ReservationCategoriesEntity> reservationCategoryMap = candidateLookupUtil.buildReservationCategoryMap(candidateProfileMap,null);

        List<CandidateOfferExcelModel> modelList = new ArrayList<>();
        for (CandidateOffersEntity entity : entities) {

            CandidateProfileEntity profile = entity.getCandidate() != null ? candidateProfileMap.get(entity.getCandidate().getId()) : null;

            String reservationCategoryCode = offerLetterGenerationUtil.resolveReservationCategoryCode(profile, reservationCategoryMap);
            String designationName = offerLetterGenerationUtil.resolveDesignationName(entity.getDesignation(), masterPositionMap);

            CandidateOfferExcelModel model = new CandidateOfferExcelModel();
            model.setOfferId(entity.getId());
            model.setRegNo(entity.getCandidateApplication() != null ? entity.getCandidateApplication().getApplicationNo() : null);
            model.setCandidateFullName(profile != null ? commonUtilityProvider.buildFullName(profile) : null);
            model.setReservationCategory(reservationCategoryCode);
            model.setDesignation(designationName);
            model.setFinalScore(entity.getInterviewSchedule() != null ? entity.getInterviewSchedule().getFinalScore() : null);
            model.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
            model.setSelectList(entity.getSelectList());
            model.setWaitList(entity.getWaitList());
//            model.setLocation(entity.getInterviewCenter() != null ? entity.getInterviewCenter().getInterviewCentre() : null);
            modelList.add(model);
        }
        return modelList;
    }

    @Transactional
    public void sendOffer(SendOfferRequestModel request)  {
        List<CandidateOffersEntity> entities = candidateOffersRepository.findAllById(request.getOfferIds());


        for (CandidateOffersEntity entity : entities) {
            entity.setTemplateId(request.getOfferTemplateId());
            entity.setJoiningDate(request.getJoiningDate());
            entity.setAcceptBeforeDate(request.getAcceptBeforeDate());
            entity.setOfferReleaseDate(LocalDate.now());
            entity.setStatus(CandidateOfferStatus.OFFER_SENT);
        }
        List<OfferMailModel> offerMailModelList = offerLetterGenerationUtil.generateOfferLetterForCandidateOffers(entities);
        candidateOffersRepository.saveAll(entities);
        List<CandidateApplicationsEntity> applications = entities.stream()
                .map(CandidateOffersEntity::getCandidateApplication)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        applications.forEach(app -> app.setApplicationStatus(CandidateApplicationStatus.OFFERED));
        candidateApplicationsRepository.saveAllWithWorkflow(applications);
        offerLetterGenerationUtil.sendOfferMails(offerMailModelList);

        // Increment offers_sent statistics for each application
        applications.forEach(app -> {
            try {
                vacancyStatisticsService.incrementOffersSent(app.getId());
            } catch (Exception e) {
                log.warn("Failed to increment offers_sent for applicationId {}: {}", app.getId(), e.getMessage());
            }
        });
    }

    @Transactional
    public void uploadOffersExcel(MultipartFile file) throws IOException {
        List<CandidateOfferExcelModel> modelList = excelTemplateService.excelToDto(file.getInputStream(), CandidateOfferExcelModel.class);

        List<String> validationErrors = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            CandidateOfferExcelModel model = modelList.get(i);
            boolean hasSelectList = model.getSelectList() != null && !model.getSelectList().isBlank();
            boolean hasWaitList = model.getWaitList() != null && !model.getWaitList().isBlank();
            if (hasSelectList && hasWaitList) {
                String regNoInfo = model.getRegNo() != null ? ", Reg No: " + model.getRegNo() : "";
                validationErrors.add("Row " + (i + 2) + regNoInfo + ": Only one of Select List or Wait List can be filled, not both.");
            }
            if (!hasSelectList && !hasWaitList) {
                String regNoInfo = model.getRegNo() != null ? ", Reg No: " + model.getRegNo() : "";
                validationErrors.add("Row " + (i + 2) + regNoInfo + ": Either Select List or Wait List must be filled.");
            }
        }
        if (!validationErrors.isEmpty()) {
            throw new ExcelValidationException(validationErrors);
        }

        List<UUID> offerIds = modelList.stream()
                .map(CandidateOfferExcelModel::getOfferId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        Map<UUID, CandidateOffersEntity> entityMap = candidateOffersRepository.findAllById(offerIds)
                .stream()
                .collect(Collectors.toMap(CandidateOffersEntity::getId, e -> e));

        List<CandidateOffersEntity> toUpdate = new ArrayList<>();
        for (CandidateOfferExcelModel model : modelList) {
            if (model.getOfferId() == null) continue;
            CandidateOffersEntity entity = entityMap.get(model.getOfferId());
            if (entity == null) continue;
            entity.setSelectList(model.getSelectList());
            entity.setWaitList(model.getWaitList());
            entity.setQualified(model.getSelectList() != null && !model.getSelectList().isEmpty());
            InterviewCentresEntity interviewCentresEntity = InterviewCentresEntity.builder().id(model.getLocation()).build();
            entity.setInterviewCenter(interviewCentresEntity);
            toUpdate.add(entity);
        }

        candidateOffersRepository.saveAll(toUpdate);
    }


    public List<MeritListDownloadModel> buildCandidateMeritList(JobPositionsEntity jobPosition, List<CandidateOfferStatus> offerStatusList,boolean hasSelectList) {
        UUID positionId = jobPosition.getId();
        JobRequisitionsEntity jobRequisition = jobRequisitionsRepository.findById(jobPosition.getRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Requisition not found"));
        List<CandidateMeritListEntity> meritListEntities = hasSelectList
                ? candidateMeritListRepository.findByPositionIdAndSelectedTrue(positionId)
                : candidateMeritListRepository.findByPositionId(positionId);
        if(meritListEntities.isEmpty()){
            throw new ResourceNotFoundException("No candidates found in merit list");
        }
//        meritListEntities.sort(
//                Comparator.comparing(
//                        CandidateMeritListEntity::getCombinedScore,
//                        Comparator.nullsLast(Comparator.reverseOrder())
//                )
//        );
        List<UUID> applicationIds = meritListEntities.stream().map(CandidateMeritListEntity::getApplicationId)
                .toList();
        List<UUID> candidateIds = meritListEntities.stream().map(CandidateMeritListEntity::getCandidateId)
                .toList();
        List<UUID> categoryIds = meritListEntities.stream().map(CandidateMeritListEntity::getCategoryId)
                .filter(Objects::nonNull).distinct()
                .toList();
        List<UUID> disabilityIds = meritListEntities.stream().map(CandidateMeritListEntity::getSelectedPwdCategoryId)
                .filter(Objects::nonNull).distinct()
                .toList();
        List<UUID> stateIds = meritListEntities.stream().map(CandidateMeritListEntity::getStateId)
                .filter(Objects::nonNull).distinct()
                .toList();
        List<UUID> cityIds = meritListEntities.stream().map(CandidateMeritListEntity::getCityId)
                .filter(Objects::nonNull).distinct()
                .toList();
        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateProfileRepository.findAllByCandidateIdIn(candidateIds).stream()
                .collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, Function.identity()));
        Map<UUID, ReservationCategoriesEntity> reservationCategoryMap = reservationCategoriesRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId, Function.identity()));
        Map<UUID, DisabilityCategoriesEntity> disabilityCategoryMap = disabilityCategoriesRepository.findAllById(disabilityIds).stream()
                .collect(Collectors.toMap(DisabilityCategoriesEntity::getId, Function.identity()));
        Map<UUID, CandidateConcessionsEntity> candidateConcessionsEntityMap = candidateConcessionsRepository.findAllByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(CandidateConcessionsEntity::getApplicationId, Function.identity()));
        List<CandidateOffersEntity> offers = candidateOffersRepository.findAllByCandidateApplication_IdInAndStatusIn(applicationIds, offerStatusList);



        Map<UUID, CandidateOffersEntity> candidateOffersEntityMap = offers.stream().collect(Collectors.toMap(
                        e -> e.getCandidateApplication().getId(),
                        Function.identity(),
                        (existing, current) -> current
                ));
        Map<UUID, String> stateMap = stateRepository.findAllById(stateIds).stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getStateName));
        Map<UUID, String> cityMap = cityRepository.findAllById(cityIds).stream()
                .collect(Collectors.toMap(CityEntity::getId, CityEntity::getCityName));

        LocalDate cutoffDate = jobRequisition != null && jobRequisition.getCutoffDate() != null ? jobRequisition.getCutoffDate() : jobPosition.getCutoffDate();
        List<MeritListDownloadModel> candidateMeritList = new ArrayList<>();
        for (CandidateMeritListEntity entity:meritListEntities) {
            CandidateOffersEntity offer = candidateOffersEntityMap.get(entity.getApplicationId());
            if (offer == null) continue;
            CandidateProfileEntity profile = candidateProfileMap.get(entity.getCandidateId());
            ReservationCategoriesEntity reservationCategory = reservationCategoryMap.get(entity.getCategoryId());
            DisabilityCategoriesEntity disabilityCategory = disabilityCategoryMap.getOrDefault(entity.getSelectedPwdCategoryId(), null);
            CandidateConcessionsEntity concession = candidateConcessionsEntityMap.getOrDefault(entity.getApplicationId(), null);

            LocalDate dob = entity.getDob();
            boolean hasDisability = entity.getIsPwd() != null ? entity.getIsPwd() : false;
            String ageConcession = concession != null ? commonUtilityProvider.getYesorNo(concession.getAgeConcession()) : AppConstants.NOT_AVAILABLE;
            String marksConcession = concession != null ? commonUtilityProvider.getYesorNo(concession.getExamConcession()) : AppConstants.NOT_AVAILABLE;
            String interviewConcession = concession != null ? commonUtilityProvider.getYesorNo(concession.getInterviewConcession()) : AppConstants.NOT_AVAILABLE;
            String selectList = offer.getSelectList() != null ? offer.getSelectList() : null;
            String waitList = offer.getWaitList() != null ? offer.getWaitList() : null;
            boolean qualified = selectList != null && !selectList.isEmpty();
            MeritListDownloadModel row = MeritListDownloadModel.builder()
                    .meritListId(entity.getId())
                    .offerId(offer.getId())
                    .letterNo(offer.getLetterNumber())
                    .candidateName(commonUtilityProvider.buildFullName(profile))
                    .registrationNo(offer.getCandidateApplication().getApplicationNo())
                    .dob(dob != null ? dob.format(AppConstants.DD_MM_YYYY) : null)
                    .cutOffDate(cutoffDate != null ? cutoffDate.format(AppConstants.DD_MM_YYYY) : null)
                    .age(commonUtilityProvider.getAgeString(cutoffDate, dob))
                    .ageConcession(ageConcession)
                    .caste(reservationCategory != null ? reservationCategory.getCategoryCode() : null)
                    .disability(commonUtilityProvider.getYesorNo(hasDisability))
                    .disabilityType(hasDisability && disabilityCategory != null ? disabilityCategory.getDisabilityCode() : null)
                    .writtenExamScore(entity.getWrittenScore() != null ? entity.getWrittenScore().toString() : null)
                    .writtenExamScoreOutOfWeightage(entity.getExamWeightedScore() != null ? entity.getExamWeightedScore().toString() : null)
                    .examConcession(marksConcession)
                    .interviewScore(entity.getInterviewScore() != null ? entity.getInterviewScore().toString() : null)
                    .interviewScoreOutOfWeightage(entity.getInterviewWeightedScore() != null ? entity.getInterviewWeightedScore().toString() : null)
                    .interviewConcession(interviewConcession)
                    .combinedScore(entity.getCombinedScore() != null ? entity.getCombinedScore().toString() : null)
                    .qualifiedOrNot(qualified ? "Q" : "NQ")
                    .selectList(selectList)
                    .waitList(waitList)
                    .state(stateMap.getOrDefault(entity.getStateId(), null))
                    .city(cityMap.getOrDefault(entity.getCityId(), null))
                    .shortlisted(commonUtilityProvider.getYesorNo(qualified))
                    .selectedReservationCategoryId(entity.getSelectedCategoryId())
                    .selectedDisabilityId(entity.getSelectedPwdCategoryId())
                    .stateId(entity.getStateId())
                    .cityId(entity.getCityId())
                    .selectedAgainstPwd(Boolean.TRUE.equals(entity.getSelectedAgainstPwd()))
                    .build();
            candidateMeritList.add(row);
        }
        candidateMeritList.sort(
                Comparator.comparing(
                                MeritListDownloadModel::getCaste,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                        .thenComparing(
                                MeritListDownloadModel::getCombinedScore,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
        );

       for(int i=0;i<candidateMeritList.size();i++){
           candidateMeritList.get(i).setSNo(i+1);
       }

        return candidateMeritList;

    }


    @Transactional(readOnly = true)
    public byte[] downloadMeritListExcel(UUID positionId) throws IOException {
        JobPositionsEntity jobPosition = jobPositionsRepository.findById(positionId).
                orElseThrow(() -> new ResourceNotFoundException("Job Position not found"));
        Set<String> hiddenColumns = new HashSet<>(Set.of("meritListId", "offerId","stateId", "cityId", "selectedReservationCategoryId","selectedDisabilityId","selectedAgainstPwd"));

        List<MeritListDownloadModel> candidateMeritList = buildCandidateMeritList(jobPosition, List.of(CandidateOfferStatus.values()),false);
        byte[] rawBytes = excelTemplateService.generateExcelTemplateWithData(MeritListDownloadModel.class, candidateMeritList, hiddenColumns).getFileContent();

        return rawBytes;
    }


    //assign positions excel download
    @Transactional
    public byte[] downloadAssignLocationExcel(UUID positionId) throws IOException {//get job
        JobPositionsEntity jobPosition = jobPositionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Position not found"));

        List<MeritListDownloadModel> candidateMeritList = buildCandidateMeritList(jobPosition, List.of(CandidateOfferStatus.OFFER_AWAITED,CandidateOfferStatus.L1_REJECTED),true);

        if(candidateMeritList.isEmpty()){
            throw new ManualValidationException("No candidates found for the given position with the specified offer statuses.");
        }


        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet mainSheet = workbook.createSheet("Merit List");

            // Parse fields via Reflection
            Field[] fields = MeritListDownloadModel.class.getDeclaredFields();
            List<Field> excelFields = getExcelFields(fields);
            List<String> headers = getHeaders(fields);


            // Build Header Row
            Row headerRow = mainSheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int stateColIdx = -1;
            int cityColIdx = -1;

            for (int i = 0; i < headers.size(); i++) {
                String headerName = headers.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerName);
                cell.setCellStyle(headerStyle);

                if ("State".equals(headerName)) stateColIdx = i;
                else if ("City".equals(headerName)) cityColIdx = i;
            }

            // Populate Main Data Rows
            int rowNum = 1;
            for (MeritListDownloadModel model : candidateMeritList) {
                Row row = mainSheet.createRow(rowNum++);
                for (int i = 0; i < excelFields.size(); i++) {
                    Field field = excelFields.get(i);
                    field.setAccessible(true);
                    try {
                        Object value = field.get(model);
                        if (value != null) {
                            row.createCell(i).setCellValue(value.toString());
                        }
                    } catch (IllegalAccessException e) {
                        // Fail-safe abstraction hook
                    }
                }
            }

            // Create Hidden Identity Sheet (Merit List ID <-> Registration No)
            Sheet mertIdsSheet = workbook.createSheet("HIDDEN_MERIT_MAPPING");
            Sheet offerIdsSheet = workbook.createSheet("HIDDEN_OFFER_MAPPING");
            Sheet selectedCategorySheet = workbook.createSheet("HIDDEN_CATEGORY_MAPPING");
            workbook.setSheetHidden(workbook.getSheetIndex(mertIdsSheet), true);
            workbook.setSheetHidden(workbook.getSheetIndex(selectedCategorySheet), true);
            workbook.setSheetHidden(workbook.getSheetIndex(offerIdsSheet), true);

            Row meritHeader = mertIdsSheet.createRow(0);
            Row categoryHeader = selectedCategorySheet.createRow(0);
            Row offerHeader = offerIdsSheet.createRow(0);
            meritHeader.createCell(0).setCellValue("Registration No");
            meritHeader.createCell(1).setCellValue("Merit List ID");
            categoryHeader.createCell(0).setCellValue("Registration No");
            categoryHeader.createCell(1).setCellValue("Category ID");
            offerHeader.createCell(0).setCellValue("Registration No");
            offerHeader.createCell(1).setCellValue("Offer ID");
            offerHeader.createCell(2).setCellValue("Letter No");

            int mRowIdx = 1;
            for (MeritListDownloadModel model : candidateMeritList) {
                Row mRow = mertIdsSheet.createRow(mRowIdx);
                Row categoryRow = selectedCategorySheet.createRow(mRowIdx);
                Row offerRow = offerIdsSheet.createRow(mRowIdx);
                mRow.createCell(0).setCellValue(model.getRegistrationNo());
                mRow.createCell(1).setCellValue(model.getMeritListId().toString());
                categoryRow.createCell(0).setCellValue(model.getRegistrationNo());
                categoryRow.createCell(1).setCellValue(model.getSelectedAgainstPwd()!=null && model.getSelectedAgainstPwd() ? model.getSelectedDisabilityId().toString() : model.getSelectedReservationCategoryId().toString());
                offerRow.createCell(0).setCellValue(model.getRegistrationNo());
                offerRow.createCell(1).setCellValue(model.getOfferId().toString());
                offerRow.createCell(2).setCellValue(model.getLetterNo());
                mRowIdx++;

            }

            // Extract and Build geographic contextual boundaries (OPTIMIZED BATCH CALLS)
            Map<String, List<String>> stateToCityMap = new HashMap<>();
            Map<String, UUID> stateNameToIdMap = new HashMap<>();
            Map<String, UUID> cityNameToIdToIdMap = new HashMap<>();

            loadGeographicContext(jobPosition, stateToCityMap, stateNameToIdMap, cityNameToIdToIdMap);

            if (stateColIdx != -1 && cityColIdx != -1 && !stateToCityMap.isEmpty()) {
                // Apply dropdown functional layouts
                applyCascadingDropdowns(workbook, mainSheet, stateToCityMap, stateColIdx, cityColIdx);

                // Generate Hidden Dictionary Sheet for safe and robust upload ID extractions
                writeGeoIdDictionarySheet(workbook, stateNameToIdMap, cityNameToIdToIdMap);

                if (jobPosition.getIsLocationWise()) {
                    writeVacancyDataSheet(workbook, jobPosition);
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                mainSheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void loadGeographicContext(JobPositionsEntity jobPosition, Map<String, List<String>> stateToCityMap,
                                       Map<String, UUID> stateNameToIdMap, Map<String, UUID> cityNameToIdMap) {
        if (!jobPosition.getIsLocationWise()) {
            List<StateEntity> allStates = stateRepository.findAll();
            List<CityEntity> allCities = cityRepository.findAll();

            Map<UUID, StateEntity> stateIdMap = allStates.stream().collect(Collectors.toMap(StateEntity::getId, Function.identity()));

            for (StateEntity state : allStates) {
                stateNameToIdMap.put(state.getStateName(), state.getId());
                stateToCityMap.put(state.getStateName(), new ArrayList<>());
            }
            for (CityEntity city : allCities) {
                if (city.getStateId() != null) {
                    StateEntity state = stateIdMap.get(city.getStateId());
                    if (state != null) {
                        stateToCityMap.get(state.getStateName()).add(city.getCityName());
                        cityNameToIdMap.put(city.getCityName(), city.getId());
                    }
                }
            }
        } else {
            List<PositionStateDistributionEntity> distributions = jobPosition.getPositionStateDistributions();

            Set<UUID> stateIds = distributions.stream().map(PositionStateDistributionEntity::getStateId).collect(Collectors.toSet());

            Map<UUID, StateEntity> statesMap = stateIds.isEmpty() ? Collections.emptyMap() :
                    stateRepository.findAllById(stateIds).stream().collect(Collectors.toMap(StateEntity::getId, Function.identity()));

            // Single data pipeline trip for all nested city objects belonging to target footprint states
            List<CityEntity> citiesPool = stateIds.isEmpty() ? Collections.emptyList() :
                    cityRepository.findByStateIdIn(stateIds);

            Map<UUID, List<CityEntity>> allCitiesByStateMap = citiesPool.stream()
                    .filter(c -> c.getStateId() != null)
                    .collect(Collectors.groupingBy(CityEntity::getStateId));

            Map<UUID, CityEntity> citiesByIdMap = citiesPool.stream()
                    .collect(Collectors.toMap(CityEntity::getId, Function.identity()));

            for (PositionStateDistributionEntity dist : distributions) {
                StateEntity state = statesMap.get(dist.getStateId());
                if (state == null) continue;

                String stateName = state.getStateName();
                stateNameToIdMap.put(stateName, state.getId());
                stateToCityMap.putIfAbsent(stateName, new ArrayList<>());

                if (dist.getCityId() == null) {
                    List<CityEntity> cities = allCitiesByStateMap.getOrDefault(dist.getStateId(), new ArrayList<>());
                    for (CityEntity city : cities) {
                        stateToCityMap.get(stateName).add(city.getCityName());
                        // Delimiter updated from "|||" to "_"
                        cityNameToIdMap.put(city.getCityName(), city.getId());
                    }
                } else {
                    CityEntity city = citiesByIdMap.get(dist.getCityId());
                    if (city != null) {
                        stateToCityMap.get(stateName).add(city.getCityName());
                        // Delimiter updated from "|||" to "_"
                        cityNameToIdMap.put(city.getCityName(), city.getId());
                    }
                }
            }
        }

        stateToCityMap.forEach((key, value) -> {
            List<String> distinct = value.stream().distinct().collect(Collectors.toList());
            stateToCityMap.put(key, distinct);
        });
    }

    private void applyCascadingDropdowns(Workbook workbook, Sheet mainSheet, Map<String, List<String>> stateToCityMap, int stateColIdx, int cityColIdx) {
        Sheet hiddenSheet = workbook.createSheet("HIDDEN_LOC_DATA");
        workbook.setSheetHidden(workbook.getSheetIndex(hiddenSheet), true);

        int colIndex = 0;
        for (Map.Entry<String, List<String>> entry : stateToCityMap.entrySet()) {
            String stateName = entry.getKey();
            List<String> cities = entry.getValue();

            Row headerRow = hiddenSheet.getRow(0);
            if (headerRow == null) headerRow = hiddenSheet.createRow(0);
            headerRow.createCell(colIndex).setCellValue(stateName);

            for (int i = 0; i < cities.size(); i++) {
                Row row = hiddenSheet.getRow(i + 1);
                if (row == null) row = hiddenSheet.createRow(i + 1);
                row.createCell(colIndex).setCellValue(cities.get(i));
            }

            // Pure regex to match the dictionary exactly
            String safeStateName = stateName.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");

            Name namedRange = workbook.createName();
            namedRange.setNameName(safeStateName);

            String colLetter = CellReference.convertNumToColString(colIndex);
            String formula = "HIDDEN_LOC_DATA!$" + colLetter + "$2:$" + colLetter + "$" + (cities.size() + 1);
            namedRange.setRefersToFormula(formula);

            colIndex++;
        }

        DataValidationHelper validationHelper = mainSheet.getDataValidationHelper();
        int maxRows = mainSheet.getLastRowNum();

        String lastColLetter = CellReference.convertNumToColString(colIndex > 0 ? colIndex - 1 : 0);
        DataValidationConstraint stateConstraint = validationHelper.createFormulaListConstraint("HIDDEN_LOC_DATA!$A$1:$" + lastColLetter + "$1");
        CellRangeAddressList stateAddressList = new CellRangeAddressList(1, maxRows, stateColIdx, stateColIdx);
        DataValidation stateValidation = validationHelper.createValidation(stateConstraint, stateAddressList);
        stateValidation.setShowErrorBox(true);
        mainSheet.addValidationData(stateValidation);

        String stateColLetter = CellReference.convertNumToColString(stateColIdx);

        // NEW CODE: Dynamic Row Count
        Sheet statemertIdsSheet = workbook.getSheet("HIDDEN_STATE_MAPPING");

        int mappingLastRow = (statemertIdsSheet != null && statemertIdsSheet.getLastRowNum() > 0)
                ? statemertIdsSheet.getLastRowNum() + 1
                : 1000;

        String indirectFormula = "INDIRECT(VLOOKUP($" + stateColLetter + "2, HIDDEN_STATE_MAPPING!$A$1:$C$" + mappingLastRow + ", 3, FALSE))";

        DataValidationConstraint cityConstraint = validationHelper.createFormulaListConstraint(indirectFormula);
        CellRangeAddressList cityAddressList = new CellRangeAddressList(1, maxRows, cityColIdx, cityColIdx);
        DataValidation cityValidation = validationHelper.createValidation(cityConstraint, cityAddressList);
        cityValidation.setEmptyCellAllowed(true);
        cityValidation.setShowErrorBox(true);
        mainSheet.addValidationData(cityValidation);
    }

    private void writeGeoIdDictionarySheet(Workbook workbook, Map<String, UUID> stateNameToIdMap, Map<String, UUID> cityNameToIdMap) {
        Sheet stateSheet = workbook.createSheet("HIDDEN_STATE_MAPPING");
        Sheet citySheet = workbook.createSheet("HIDDEN_CITY_MAPPING");
        workbook.setSheetHidden(workbook.getSheetIndex(stateSheet), true);
        workbook.setSheetHidden(workbook.getSheetIndex(citySheet), true);

        Row stateHeader = stateSheet.createRow(0);
        stateHeader.createCell(0).setCellValue("State Name");
        stateHeader.createCell(1).setCellValue("State UUID");
        stateHeader.createCell(2).setCellValue("Safe Name"); // The translation column

        Row cityHeader = citySheet.createRow(0);
        cityHeader.createCell(0).setCellValue("City Name");
        cityHeader.createCell(1).setCellValue("City UUID");

        int stateRowIdx = 1;
        for (Map.Entry<String, UUID> entry : stateNameToIdMap.entrySet()) {
            Row row = stateSheet.getRow(stateRowIdx);
            if (row == null) row = stateSheet.createRow(stateRowIdx);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().toString());

            // Pure regex, no prefix needed based on your guarantee
            String safeName = entry.getKey().replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
            row.createCell(2).setCellValue(safeName);

            stateRowIdx++;
        }

        int cityRowIdx = 1;
        for (Map.Entry<String, UUID> entry : cityNameToIdMap.entrySet()) {
            Row row = citySheet.getRow(cityRowIdx);
            if (row == null) row = citySheet.createRow(cityRowIdx);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().toString());
            cityRowIdx++;
        }
    }

    private void writeVacancyDataSheet(Workbook workbook, JobPositionsEntity jobPosition) {
        Sheet vacancySheet = workbook.createSheet("HIDDEN_VACANCY_DATA");
        workbook.setSheetHidden(workbook.getSheetIndex(vacancySheet), true);

        Row header = vacancySheet.createRow(0);
        header.createCell(0).setCellValue("Vacancy Key");
        header.createCell(1).setCellValue("Vacancy Count");
        header.createCell(2).setCellValue("State Id");
        header.createCell(3).setCellValue("Has City");

        List<PositionStateDistributionEntity> distributions = jobPosition.getPositionStateDistributions();
        if (distributions.isEmpty()) return;

        int rowIdx = 1;
        for (PositionStateDistributionEntity dist : distributions) {
            UUID stateId = dist.getStateId();
            UUID cityId = dist.getCityId() != null ? dist.getCityId() : null;

            for (PositionCategoryDistributionEntity catDist : dist.getPositionCategoryDistributions()) {
                String key = buildVacancyKey(stateId,cityId,catDist.getReservationCategoryId(),catDist.getDisabilityCategoryId());
                Row row = vacancySheet.createRow(rowIdx);
                row.createCell(0).setCellValue(key);
                row.createCell(1).setCellValue(catDist.getVacancyCount());
                row.createCell(2).setCellValue(stateId.toString());
                row.createCell(3).setCellValue(cityId!=null ? AppConstants.YES : AppConstants.NO);
                rowIdx++;
            }
        }
    }
    //assign positions excel end


    //assign positions excel upload
    @Transactional
    public void uploadOffersExcelNew(MultipartFile file){
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            // 1. Extract hidden tracking sheet metadata into lightweight maps
            Map<String, UUID> regNoToMeritIdMap = buildSimpleLookup(workbook.getSheet("HIDDEN_MERIT_MAPPING"));
            Map<String, UUID> regNoToOfferIdMap = buildSimpleLookup(workbook.getSheet("HIDDEN_OFFER_MAPPING"));
            Map<String,UUID> regNotToSelcetedCategoryIdMap = buildSimpleLookup(workbook.getSheet("HIDDEN_CATEGORY_MAPPING"));
            Map<String, UUID> stateMap = buildSimpleLookup(workbook.getSheet("HIDDEN_STATE_MAPPING"));
            Map<String, UUID> cityMap = buildSimpleLookup(workbook.getSheet("HIDDEN_CITY_MAPPING"));
            Map<String, Integer> vacancyMap = buildVacancyCountMap(workbook.getSheet("HIDDEN_VACANCY_DATA"));
            Map<UUID,Boolean>  stateWithCityVacancyMap = buildStateHasCityMap(workbook.getSheet("HIDDEN_VACANCY_DATA"));
            Map<String,Set<String>> stateToCitiesMap = buildStateToCitiesMap(workbook.getSheet("HIDDEN_LOC_DATA"));

            Set<String> existingLetterNosInDB = candidateOffersRepository.findOfferLetterNumbers(regNoToOfferIdMap.values());


            List<MeritListDownloadModel> modelList = parseMainSheetRows(workbook.getSheetAt(0), regNoToMeritIdMap, regNoToOfferIdMap,regNotToSelcetedCategoryIdMap, stateMap, cityMap,stateToCitiesMap,existingLetterNosInDB);
            List<UUID> selectedReservationCategoryIds = modelList.stream().map(MeritListDownloadModel::getSelectedReservationCategoryId).filter(Objects::nonNull).toList();
            List<UUID> selectedDisabilityIds = modelList.stream().map(MeritListDownloadModel::getSelectedDisabilityId).filter(Objects::nonNull).toList();
            Map<UUID,String> selectedReservationCategoryMap = reservationCategoriesRepository.findAllById(selectedReservationCategoryIds).stream()
                    .collect(Collectors.toMap(
                            ReservationCategoriesEntity::getId,
                            ReservationCategoriesEntity::getCategoryCode
                    ));
            Map<UUID,String> selectedDisabilityMap = disabilityCategoriesRepository.findAllById(selectedDisabilityIds).stream()
                    .collect(Collectors.toMap(
                            DisabilityCategoriesEntity::getId,
                            DisabilityCategoriesEntity::getDisabilityCode
                    ));

            List<String> errors = new ArrayList<>();

            if(!vacancyMap.isEmpty()){
                Map<String, List<MeritListDownloadModel>> allocationCountMap = buildVacanciesAllocateMap(modelList,stateWithCityVacancyMap);
                allocationCountMap.forEach((key, groupedList) -> {
                    int allocatedCount = groupedList.size();
                    Integer allowedCount = vacancyMap.getOrDefault(key, 0);
                    if (allocatedCount > allowedCount) {
                        MeritListDownloadModel sample = groupedList.get(0);
                        String categoryName;

                        if (sample.getSelectedDisabilityId() != null) {
                            categoryName = selectedDisabilityMap.get(sample.getSelectedDisabilityId());
                        } else {
                            categoryName = selectedReservationCategoryMap.get(sample.getSelectedReservationCategoryId());
                        }

                        errors.add(
                                String.format(
                                        "Vacancy exceeded for %s%s under %s category. Allowed vacancies: %d, Assigned candidates: %d.",
                                        sample.getState(),
                                        sample.getCity() != null ? "-" + sample.getCity() : "",
                                        categoryName,
                                        allowedCount,
                                        allocatedCount
                                )
                        );

                    }

                });
            }

            if(!errors.isEmpty()){
                throw new ExcelValidationException(errors);
            }
            List<UUID> meritListIds = modelList.stream().map(MeritListDownloadModel::getMeritListId).toList();
            List<UUID> offerIds = modelList.stream().map(MeritListDownloadModel::getOfferId).toList();

            Map<UUID,CandidateMeritListEntity> meritListEntityMap = candidateMeritListRepository.findAllById(meritListIds)
                    .stream().collect(Collectors.toMap(
                            CandidateMeritListEntity::getId,
                            Function.identity()
                    ));
            Map<UUID,CandidateOffersEntity> candidateOffersEntityMap = candidateOffersRepository.findAllById(offerIds).stream()
                    .collect(Collectors.toMap(CandidateOffersEntity::getId, Function.identity()));


            List<CandidateMeritListEntity> savingMeritList = modelList.stream()
                            .map(curr -> {
                                CandidateMeritListEntity meritEntity = meritListEntityMap.get(curr.getMeritListId());

                                if (meritEntity == null) {
                                    return null;
                                }
                                meritEntity.setStateId(curr.getStateId());
                                meritEntity.setCityId(curr.getCityId());
                                return meritEntity;
                            })
                            .filter(Objects::nonNull)
                            .toList();
            List<CandidateOffersEntity> savingOffers = modelList.stream()
                    .map(curr -> {
                        CandidateOffersEntity offerEntity = candidateOffersEntityMap.get(curr.getOfferId());

                        if (offerEntity == null) {
                            return null;
                        }
                        offerEntity.setLetterNumber(curr.getLetterNo());
                        return offerEntity;
                    })
                    .filter(Objects::nonNull)
                    .toList();


            candidateMeritListRepository.saveAll(savingMeritList);
            candidateOffersRepository.saveAll(savingOffers);
        }catch (IOException exception){
            throw new ManualValidationException("Unexpected Error.Please try again.");
        }
    }

    private Map<String, UUID> buildSimpleLookup(Sheet sheet) {
        Map<String, UUID> map = new HashMap<>();
        if (sheet == null) return map;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValue(row,0);
            String idStr = getCellValue(row,1);
            if(name == null || idStr == null) continue;
            map.put(name, UUID.fromString(idStr));
        }
        return map;
    }

    private Map<String,Integer> buildVacancyCountMap(Sheet sheet) {
        Map<String, Integer> map = new HashMap<>();
        if (sheet == null) return map;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValue(row,0);
            String vacanyCount = getCellValue(row,1);
            if(name == null || vacanyCount == null) continue;
            map.put(name, Integer.parseInt(vacanyCount));
        }
        return map;
    }

    private Map<UUID,Boolean> buildStateHasCityMap(Sheet sheet){
        Map<UUID,Boolean> map = new HashMap<>();
        if (sheet == null) return map;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String stateIdStr = getCellValue(row,2);
            String hasCity = getCellValue(row,3);
            if( stateIdStr == null || hasCity == null) continue;
            map.put(UUID.fromString(stateIdStr),AppConstants.YES.equals(hasCity));
        }
        return map;
    }

    private Map<String, Set<String>> buildStateToCitiesMap(Sheet hiddenSheet) {

        Map<String, Set<String>> stateToCityMap = new LinkedHashMap<>();

        Row headerRow = hiddenSheet.getRow(0);
        if (headerRow == null) {
            return stateToCityMap;
        }

        int lastColumn = headerRow.getLastCellNum();

        for (int col = 0; col < lastColumn; col++) {

            String stateName = getCellValue(headerRow, col);

            if (stateName == null || stateName.isBlank()) {
                continue;
            }

            Set<String> cities = new HashSet<>();

            for (int rowNum = 1; rowNum <= hiddenSheet.getLastRowNum(); rowNum++) {

                Row row = hiddenSheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }

                String cityName = getCellValue(row, col);

                if (cityName != null && !cityName.isBlank()) {
                    cities.add(cityName);
                }
            }

            stateToCityMap.put(stateName, cities);
        }

        return stateToCityMap;
    }

    private Map<String, List<MeritListDownloadModel>> buildVacanciesAllocateMap(List<MeritListDownloadModel> meritList, Map<UUID, Boolean> stateWithCityMap) {

        return meritList.stream()
                .collect(Collectors.groupingBy(model -> {

                    UUID stateId = model.getStateId();
                    boolean cityWise = stateWithCityMap.getOrDefault(stateId, false);

                    return buildVacancyKey(
                            stateId,
                            cityWise ? model.getCityId() : null,
                            model.getSelectedReservationCategoryId(),
                            model.getSelectedDisabilityId()
                    );
                }));
    }




    private List<MeritListDownloadModel> parseMainSheetRows(Sheet sheet, Map<String, UUID> identityMap,Map<String,UUID> offerMap,
                                                            Map<String,UUID> categoryMap, Map<String, UUID> stateMap,
                                                            Map<String, UUID> cityMap,Map<String,Set<String>> stateToCitiesMap,
                                                            Set<String> existingOfferLetterNumbersInDB) {
        List<MeritListDownloadModel> rows = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        if (!rowIterator.hasNext()) throw new ExcelValidationException(List.of("Please Enter a Valid Excel"));

        Row headerRow = rowIterator.next();
        Map<String, Integer> headerMap = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                headerMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
        }
        List<String> expectedHeaders = getHeaders(MeritListDownloadModel.class.getDeclaredFields());
        validateHeaders(expectedHeaders,headerMap.keySet());
        Set<String> offerLetterNumbersInExcel = new HashSet<>();
        List<String> errors =  new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row == null || row.getPhysicalNumberOfCells() == 0) continue;


            String stateStr = getCellValue(row, headerMap.get("State"));
            String cityStr = getCellValue(row, headerMap.get("City"));
            String letterNo = getCellValue(row, headerMap.get("Offer Letter No"));
            String regNoStr = getCellValue(row, headerMap.get("Registration No"));

            if(stateStr == null){
                errors.add("Row " + (row.getRowNum() + 1)+" : state cannot be empty");
            }

            if(cityStr == null){
                errors.add("Row " + (row.getRowNum() + 1)+" : city cannot be empty");
            }

            if(letterNo == null){
                errors.add("Row " + (row.getRowNum() + 1)+" : letter no cannot be empty");
            }

            if(letterNo!=null && existingOfferLetterNumbersInDB.contains(letterNo)){
                errors.add("Row " + (row.getRowNum() + 1)+" : letter no '"+letterNo+"' already exists for other candidate application");
            }

            if(letterNo!=null && offerLetterNumbersInExcel.contains(letterNo)){
                errors.add("Row " + (row.getRowNum() + 1)+" : Letter no '"+letterNo+"' is duplicated in the excel");
            }

            if (letterNo!=null && !letterNo.matches(RegexPattern.ALPHA_NUMERIC_DASH.getRegex())){
                errors.add("Row " + (row.getRowNum() + 1)
                        + " : Letter number " + letterNo +" is invalid."
                        + " Letter number can contain only letters, numbers, and hyphens (-).");
            }

            //adding letterNo here to check for duplicates in the same excel
            offerLetterNumbersInExcel.add(letterNo);

            if(stateStr!=null && cityStr!=null && !stateToCitiesMap.isEmpty()){
               Set<String> stateCityNames = stateToCitiesMap.getOrDefault(stateStr,null);
                if(stateCityNames != null && !stateCityNames.contains(cityStr)) {
                    errors.add(
                            "Row " + (row.getRowNum() + 1) +
                                    ": State '" + stateStr +
                                    "' does not have city '" + cityStr + "'"
                    );
                }


            }

            if(errors.isEmpty()){

                String disability = getCellValue(row,headerMap.get("disability"));

                UUID resolvedMeritListId = identityMap.get(regNoStr);
                UUID resolvedOfferId = offerMap.get(regNoStr);
                UUID selectedCategoryId = null;
                UUID selectedDisabilityId = null;
                if(AppConstants.YES.equals(disability)){
                    selectedDisabilityId = categoryMap.get(regNoStr);
                }else{
                    selectedCategoryId = categoryMap.get(regNoStr);
                }
                if (resolvedMeritListId == null || resolvedOfferId == null) continue;

                UUID stateId = stateMap.getOrDefault(stateStr,null);
                UUID cityId =  cityMap.getOrDefault(cityStr,null);

                rows.add(MeritListDownloadModel.builder()
                        .meritListId(resolvedMeritListId)
                        .offerId(resolvedOfferId)
                        .registrationNo(regNoStr)
                        .state(stateStr)
                        .city(cityStr)
                        .stateId(stateId)
                        .cityId(cityId)
                        .selectedReservationCategoryId(selectedCategoryId)
                        .selectedDisabilityId(selectedDisabilityId)
                        .letterNo(letterNo)
                        .build());
            }

        }
        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }
        if (rows.isEmpty()) {
            throw new ExcelValidationException(List.of("Excel file contains no data rows."));
        }
        return rows;
    }



    private String getCellValue(Row row, Integer cellIndex) {
        if (cellIndex == null) return null;
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long)cell.getNumericCellValue());
            default -> null;
        };
    }

    private String buildVacancyKey(
            UUID stateId,
            UUID cityId,
            UUID categoryId,
            UUID disabilityId) {

        return Stream.of(stateId, cityId, categoryId, disabilityId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.joining("_"));
    }

    private List<String> getHeaders(Field[] fields){
        List<String> headers = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ExcelHeader.class)) {
                headers.add(field.getAnnotation(ExcelHeader.class).value());
            }
        }
        return headers;

    }

    public List<Field> getExcelFields(Field[] fields){
        List<Field> excelFields = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ExcelHeader.class)) {
                excelFields.add(field);
            }
        }
        return excelFields;
    }

    public void validateHeaders(List<String> expectedHeaders,Set<String> actualHeaders){
        if(!expectedHeaders.equals(new ArrayList<>(actualHeaders))){
            throw  new ExcelValidationException(List.of("Headers mismatch.Please upload a valid excel"));
        }
    }


    @Transactional
    public void sendOfferToApproval(SendOfferRequestModel request) {
        // 1. Fetch & Update Candidate Offers
        List<CandidateOffersEntity> offers = candidateOffersRepository.findAllById(request.getOfferIds());
        UUID userId=securityUtils.getCurrentUserId();
        offers.forEach(offer -> {
            offer.setTemplateId(request.getOfferTemplateId());
            offer.setJoiningDate(request.getJoiningDate());
            offer.setAcceptBeforeDate(request.getAcceptBeforeDate());
            offer.setOfferReleaseDate(LocalDate.now());
            offer.setStatus(CandidateOfferStatus.L1_PENDING);
        });

        // Generate the offer letters
        offerLetterGenerationUtil.generateOfferLetterForCandidateOffers(offers);

        // Save updated offers and extract Application IDs
        List<CandidateOffersEntity> savedOffers = candidateOffersRepository.saveAll(offers);
        List<UUID> applicationIds = savedOffers.stream()
                .map(o -> o.getCandidateApplication().getId())
                .toList();

        // 2. Fetch Existing Approvals
        Map<UUID, OfferApprovalsEntity> existingApprovalMap = offerApprovalRepository
                .findByOfferIdIn(request.getOfferIds()).stream()
                .collect(Collectors.toMap(OfferApprovalsEntity::getOfferId, Function.identity()));

        // 3. Build & Save Offer Approvals
        List<OfferApprovalsEntity> approvalsToSave = savedOffers.stream().map(offer -> {
            // Note: We use setters instead of a Builder here. If this entity already exists in the DB,
            // using a Builder would create a detached object and break JPA's UPDATE mechanism.
            OfferApprovalsEntity approval = existingApprovalMap.getOrDefault(offer.getId(), new OfferApprovalsEntity());

            approval.setOfferId(offer.getId());
            approval.setCandidateApplication(offer.getCandidateApplication());
            approval.setCandidateId(offer.getCandidate().getId());
            approval.setDesignationId(offer.getDesignation());
            approval.setCtc(offer.getCtc());
            approval.setBonus(offer.getBonus());
            approval.setJoiningDate(offer.getJoiningDate());
            approval.setOfferReleaseDate(offer.getOfferReleaseDate());
            approval.setAcceptBeforeDate(offer.getAcceptBeforeDate());
            approval.setTemplateId(offer.getTemplateId());
            approval.setOfferFileUrl(offer.getOfferFileUrl());
            approval.setApprovalStatus(OfferApprovalStatus.L1_PENDING);
            return approval;

        }).toList();

        List<OfferApprovalsEntity> savedApprovals = offerApprovalRepository.saveAll(approvalsToSave);
        List<WorkflowApprovalEntity> workflowApprovalsToSave = savedApprovals.stream()
                .map(approval -> buildWorkflowEntity(approval, userId))
                .toList();
        workflowApprovalEntityRepository.saveAll(workflowApprovalsToSave);

//        // 4. Fetch Merit List map for State/City IDs
//        Map<UUID, CandidateMeritListEntity> meritMap = candidateMeritListRepository
//                .findByApplicationIdIn(applicationIds).stream()
//                .collect(Collectors.toMap(CandidateMeritListEntity::getApplicationId, Function.identity()));
//
//        // 5. Build History using the Builder Pattern
//        List<OfferApprovalHistoryEntity> historyToSave = savedApprovals.stream().map(approval -> {
//            CandidateMeritListEntity merit = meritMap.get(approval.getCandidateApplication().getId());
//
//            // Because History is always a brand-new insert, the Builder pattern is perfect here
//            return OfferApprovalHistoryEntity.builder()
//                    .offerApprovalId(approval.getId())
//                    .offerId(approval.getOfferId())
//                    .candidateApplication(approval.getCandidateApplication())
//                    .candidateId(approval.getCandidateId())
//                    .designationId(approval.getDesignationId())
//                    .ctc(approval.getCtc())
//                    .bonus(approval.getBonus())
//                    .joiningDate(approval.getJoiningDate())
//                    .offerReleaseDate(approval.getOfferReleaseDate())
//                    .acceptBeforeDate(approval.getAcceptBeforeDate())
//                    .templateId(approval.getTemplateId())
//                    .offerFileUrl(approval.getOfferFileUrl())
//                    .approvalStatus(approval.getApprovalStatus())
//                    // Safely handles nulls if a candidate doesn't have a merit list entry
//                    .stateId(merit != null ? merit.getStateId() : null)
//                    .cityId(merit != null ? merit.getCityId() : null)
//                    // .actionBy(currentUserId)
//                    .build();
//        }).collect(Collectors.toList());
//
//        // 6. Save the ledger history
//        offerApprovalHistoryRepository.saveAllWithWorkflow(historyToSave, null);
    }

    private WorkflowApprovalEntity buildWorkflowEntity(OfferApprovalsEntity offerApprovals,UUID userId){
        WorkflowApprovalEntity workflow = WorkflowApprovalEntity.builder()
                .entityId(offerApprovals.getId())
                .entityType(OfferApprovalsEntity.ENTITY_TYPE)
                .stepNumber(1)
                .approverRole("")
                .approverId(userId)
                .action(offerApprovals.getApprovalStatus().toString())
                .actionDate(LocalDateTime.now())
                .comments(null)
                .status(offerApprovals.getApprovalStatus().toString())
                .build();
        return workflow;
    }

    public void generateOffers(SendOfferRequestModel request) {
        List<CandidateOffersEntity> offers = candidateOffersRepository.findAllById(request.getOfferIds());
        List<CandidateOffersEntity> offersWithoutLetterNo = offers.stream().
                filter(o -> o.getLetterNumber() == null || o.getLetterNumber().isBlank())
                .toList();

        if(!offersWithoutLetterNo.isEmpty()){

            throw new ManualValidationException("Offer Letter Number is missing for selected applications. Please download assign locations excel, to assign missing letter numbers, and upload before generating offer letters.");
        }

        offers.forEach(offer -> {
            offer.setTemplateId(request.getOfferTemplateId());
            offer.setJoiningDate(request.getJoiningDate());
            offer.setAcceptBeforeDate(request.getAcceptBeforeDate());
            offer.setOfferReleaseDate(LocalDate.now());
            offer.setSignatoryName(request.getSignatoryName());
            offer.setSignatoryDesignation(request.getSignatoryDesignation());
            offer.setStatus(CandidateOfferStatus.OFFER_GENERATED);
        });

        offerLetterGenerationUtil.generateOfferLetterForCandidateOffers(offers);
        candidateOffersRepository.saveAll(offers);
    }

    public void streamOffersAsZip(List<UUID> offerIds, ServletOutputStream outputStream) {
        List<CandidateOffersEntity> candidateOffers = candidateOffersRepository.findAllById(offerIds);
        if (candidateOffers.isEmpty()) {
            throw new CommonException("No candidate offers found for the provided IDs.");
        }

        List<String> offerUrls = candidateOffers.stream()
                .map(CandidateOffersEntity::getOfferFileUrl)
                .filter(Objects::nonNull)
                .filter(url -> !url.trim().isEmpty())
                .toList();

        if (offerUrls.isEmpty()) {
            throw new CommonException("None of the selected candidates have generated offer letters yet.");
        }

        azureBlobStorageService.downloadFilesAsZip(offerUrls, outputStream);
    }

    public Map<String, CandidateOffersEntity> loadAndBuildOfferMapByUrls(UUID positionId,List<String> fileUrls) {
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findByJobPosition_IdAndOfferFileUrlIn(positionId,fileUrls);

        return candidateOffersEntities.stream()
                .filter(e -> e.getOfferFileUrl() != null)
                .collect(Collectors.toMap(
                        e -> e.getOfferFileUrl().trim(),
                        Function.identity()
                ));
    }

    @Transactional
    public Map<String, Object> processSignedOffersZip(UUID positionId,MultipartFile zipFile) {
        String standardizedPath = "/" + offerUploadPath.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        List<String> expectedUrls = new ArrayList<>();

        // ----------------------------------------------------------------------
        // PASS 1: Read ZIP directory structure to build expected URLs
        // ----------------------------------------------------------------------
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".pdf")) {
                    zis.closeEntry();
                    continue;
                }
                String filename = entry.getName().replace("\\", "/");
                String finalName = filename.substring(filename.lastIndexOf('/') + 1);
                String finalUrl = standardizedPath + "/" + finalName;
                expectedUrls.add(finalUrl);
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to read the uploaded zip file during initial scan", e);
            throw new CommonException("Failed to scan the zip file.");
        }

        if (expectedUrls.isEmpty()) {
            throw new CommonException("No valid PDF files found in the ZIP.");
        }

        Map<String, CandidateOffersEntity> candidateOffersEntityMap = loadAndBuildOfferMapByUrls(positionId,expectedUrls);

        int successCount = 0;
        int failureCount = 0;
        List<String> successfulApplicationNumbers = new ArrayList<>();
        List<CandidateOffersEntity> successfullyUpdatedOffers = new ArrayList<>();

        // ----------------------------------------------------------------------
        // PASS 2: Re-read the ZIP to extract bytes and process uploads
        // ----------------------------------------------------------------------
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".pdf")) {
                    zis.closeEntry();
                    continue;
                }

                String filename = entry.getName().replace("\\", "/");
                String finalName = filename.substring(filename.lastIndexOf('/') + 1);
                String finalUrl = standardizedPath + "/" + finalName;

                CandidateOffersEntity offer = candidateOffersEntityMap.get(finalUrl);

                if (offer != null) {
                    try {
                        byte[] signedPdfData = zis.readAllBytes();
                        MultipartFile pdfFile = new CustomMultipartFile(signedPdfData, "file", finalName, "application/pdf");

                        // Delete the old unsigned PDF from Azure
                        if (offer.getOfferFileUrl() != null) {
                            String oldFilename = offer.getOfferFileUrl().substring(offer.getOfferFileUrl().lastIndexOf('/') + 1);
                            azureBlobStorageService.deleteFile(offerUploadPath, oldFilename);
                        }

                        // Upload new file and update path
                        String newBlobPath = azureBlobStorageService.uploadFile(pdfFile, finalName, offerUploadPath);

                        // FIX 1: Use standardizedPath here!
                        offer.setOfferFileUrl(standardizedPath + "/" + newBlobPath);
                        offer.setStatus(CandidateOfferStatus.L1_PENDING);

                        // FIX 2 & 3: Do NOT save to DB in the loop. Just gather them in memory.
                        successfullyUpdatedOffers.add(offer);

                        if (offer.getCandidateApplication() != null && offer.getCandidateApplication().getApplicationNo() != null) {
                            successfulApplicationNumbers.add(offer.getCandidateApplication().getApplicationNo());
                        } else {
                            successfulApplicationNumbers.add("Offer ID: " + offer.getId().toString());
                        }

                        successCount++;

                    } catch (Exception e) {
                        log.error("Failed to process file: {}", filename, e);
                        failureCount++;
                    }
                } else {
                    log.warn("Entity mapping skipped. No candidate found in database for normalized path key: {}", finalUrl);
                    failureCount++;
                }

                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to read the uploaded zip file during extraction", e);
            throw new CommonException("Failed to extract files from the zip.");
        }

        if (!successfullyUpdatedOffers.isEmpty()) {
            submitOffersToWorkflowPool(successfullyUpdatedOffers);
        }
        // Return a map containing the stats AND the in-memory entities that need to be saved
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("successfulApplicationNumbers", successfulApplicationNumbers);

        return result;
    }


    public void submitOffersToWorkflowPool(List<CandidateOffersEntity> offers) {
        if (offers == null || offers.isEmpty()) return;

        // 1. Bulk save all 500 offers in ONE database call
        candidateOffersRepository.saveAll(offers);

        List<UUID> offerIds = offers.stream()
                .map(CandidateOffersEntity::getId)
                .toList();

        // 2. Fetch existing approvals
        Map<UUID, OfferApprovalsEntity> existingApprovalMap = offerApprovalRepository
                .findByOfferIdIn(offerIds).stream()
                .collect(Collectors.toMap(OfferApprovalsEntity::getOfferId, Function.identity()));

        // 3. Build approvals in memory
        List<OfferApprovalsEntity> approvalsToSave = offers.stream().map(offer -> {
            OfferApprovalsEntity approval = existingApprovalMap.getOrDefault(offer.getId(), new OfferApprovalsEntity());

            approval.setOfferId(offer.getId());
            approval.setCandidateApplication(offer.getCandidateApplication());
            approval.setCandidateId(offer.getCandidate().getId());
            approval.setDesignationId(offer.getDesignation());
            approval.setCtc(offer.getCtc());
            approval.setBonus(offer.getBonus());
            approval.setJoiningDate(offer.getJoiningDate());
            approval.setOfferReleaseDate(offer.getOfferReleaseDate());
            approval.setAcceptBeforeDate(offer.getAcceptBeforeDate());
            approval.setTemplateId(offer.getTemplateId());
            approval.setOfferFileUrl(offer.getOfferFileUrl());
            approval.setApprovalStatus(OfferApprovalStatus.L1_PENDING);
            return approval;
        }).toList();

        // 4. Bulk save all 500 approvals in ONE database call
        List<OfferApprovalsEntity> savedApprovals = offerApprovalRepository.saveAll(approvalsToSave);

        // Fetch user ID ONCE to avoid N+1 queries in the loop
        UUID currentUserId = securityUtils.getCurrentUserId();

        List<WorkflowApprovalEntity> workflowApprovalsToSave = savedApprovals.stream()
                // Update your buildWorkflowEntity method to accept currentUserId as a parameter!
                .map(approval -> buildWorkflowEntity(approval, currentUserId))
                .toList();

        // 5. Bulk save all 500 workflows in ONE database call
        workflowApprovalEntityRepository.saveAll(workflowApprovalsToSave);
    }
}













