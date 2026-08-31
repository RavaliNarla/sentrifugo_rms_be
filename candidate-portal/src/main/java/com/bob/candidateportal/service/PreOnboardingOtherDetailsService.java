package com.bob.candidateportal.service;

import com.bob.candidateportal.model.PreOnboardingOtherDetailsModel;
import com.bob.candidateportal.model.PreOnboardingQualificationModel;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.FileValidationUtil;
import com.bob.db.dto.PreOnboardingCertificationDTO;
import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.dto.PreOnboardingOtherDetailsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.PreOnboardingDocumentType;
import com.bob.db.mapper.PreOnboardingDocumentMapper;
import com.bob.db.mapper.PreOnboardingOtherDetailsMapper;
import com.bob.db.mapper.PreOnboardingReferenceMapper;
import com.bob.db.repository.*;
import jakarta.persistence.Access;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PreOnboardingOtherDetailsService {

    @Autowired
    private PreOnboardingOtherDetailsRepository preOnboardingOtherDetailsRepository;

    @Autowired
    private PreOnboardingOtherDetailsMapper preOnboardingOtherDetailsMapper;

    @Autowired
    private PreOnboardingReferenceMapper preOnboardingReferenceMapper;

    @Autowired
    private PreOnboardingReferenceRepository preOnboardingReferenceRepository;

    @Autowired
    private PreOnboardingDocumentRepository preOnboardingDocumentRepository;

    @Autowired
    private PreOnboardingDocumentMapper preOnboardingDocumentMapper;

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;

    private FileValidationUtil fileValidationUtil;

    @Value("${candidate.document.upload.path}")
    private String canDocUploadPath;

    @Autowired
    private FileService fileService;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    public PreOnboardingOtherDetailsModel getOtherDetailsByAppId(UUID applicationId) {
        Optional<CandidateApplicationsEntity> candidateApplicationsEntityOpt=candidateApplicationsRepository.findById(applicationId);
        if(!candidateApplicationsEntityOpt.isPresent()){
            throw new ManualValidationException("Application not found");
        }
        CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityOpt.get();
        CandidateParentModel candidateParentModel=candidateCommonGetService.getCandidateBasicDetails(candidateApplications.getCandidateId(),candidateApplications.getPositionId());

        Optional<PreOnboardingEntity> preOnboardingEntityOpt=preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity=preOnboardingEntityOpt.get();
        Optional<PreOnboardingOtherDetailsEntity> preOnbOtherDetailsOpt=preOnboardingOtherDetailsRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingOtherDetailsEntity otherDetails=null;
        if(preOnbOtherDetailsOpt.isPresent()){
            otherDetails=preOnbOtherDetailsOpt.get();
        }
        List<PreOnboardingReferenceEntity> preOnboardingReferenceEntityList=preOnboardingReferenceRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        List<PreOnboardingDocumentEntity> preOnboardingDocumentEntities=preOnboardingDocumentRepository.findByPreOnboardingIdAndDocumentTypeIn(preOnboardingEntity.getId(),List.of(PreOnboardingDocumentType.OTHER));
        return PreOnboardingOtherDetailsModel.builder()
                .candidateParentModel(candidateParentModel)
                .preOnboardingOtherDetails(preOnboardingOtherDetailsMapper.toDTO(otherDetails))
                .preOnboardingDocuments(preOnboardingDocumentMapper.toDTOList(preOnboardingDocumentEntities))
                .preOnboardingReferences(preOnboardingReferenceMapper.toDTOList(preOnboardingReferenceEntityList))
                .build();
    }

    public void saveOrUpdateOtherDetails(UUID applicationId, PreOnboardingOtherDetailsModel otherDetailsModel) {
        Optional<CandidateApplicationsEntity> candidateApplicationsEntityOpt=candidateApplicationsRepository.findById(applicationId);
        if(!candidateApplicationsEntityOpt.isPresent()){
            throw new ManualValidationException("Application not found");
        }
        CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityOpt.get();
        Optional<PreOnboardingEntity> preOnboardingEntityOpt=preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity=preOnboardingEntityOpt.get();
        //Save or update the preOnboardingOther details
        Optional<PreOnboardingOtherDetailsEntity> preOnbOtherDetailsOpt=preOnboardingOtherDetailsRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingOtherDetailsEntity preOnboardingOtherDetailsEntity=null;
        if(!preOnbOtherDetailsOpt.isPresent()){
            preOnboardingOtherDetailsEntity=preOnboardingOtherDetailsMapper.toEntity(otherDetailsModel.getPreOnboardingOtherDetails());
            preOnboardingOtherDetailsEntity.setPreOnboardingId(preOnboardingEntity.getId());
        }
        else{
            preOnboardingOtherDetailsEntity=preOnbOtherDetailsOpt.get();
            preOnboardingOtherDetailsMapper.updateEntityFromDto(otherDetailsModel.getPreOnboardingOtherDetails(),preOnboardingOtherDetailsEntity);
        }
        // save or update the other details
        preOnboardingOtherDetailsRepository.save(preOnboardingOtherDetailsEntity);
        // Save or update Refererences
        List<PreOnboardingReferenceEntity> oldOnboardingReferenceEntities=preOnboardingReferenceRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        // Delete the existing entries
        preOnboardingReferenceRepository.deleteAll(oldOnboardingReferenceEntities);
        //Save the new entries
        List<PreOnboardingReferenceEntity> newOnboardingReferenceEntities=preOnboardingReferenceMapper.toEntityList(otherDetailsModel.getPreOnboardingReferences());
        newOnboardingReferenceEntities.forEach(nR->nR.setPreOnboardingId(preOnboardingEntity.getId()));
        preOnboardingReferenceRepository.saveAll(newOnboardingReferenceEntities);
        if(candidateApplications.getStepper()==4){
            candidateApplications.setApplicationStatus(CandidateApplicationStatus.PRE_ONBOARDING_COMPLETED);
            candidateApplications.setStepper(null);
            candidateApplicationsRepository.save(candidateApplications);
        }
    }

//    private void saveOrUpdateReferenceDetails(PreOnboardingOtherDetailsModel otherDetailsModel, PreOnboardingEntity preOnboardingEntity) {
//
//        List<PreOnboardingO> currentCertifications = otherDetailsModel.getPreOnboardingCertifications();
//        if (currentCertifications == null) {
//            return;
//        }
//        List<PreOnboardingCertificationEntity> savedCertifications = preOnboardingCertificationRepository
//                .findByPreOnboardingId(preOnboardingEntity.getId());
//
//        List<PreOnboardingCertificationEntity> entitiesToSave = new ArrayList<>();
//
//        Set<UUID> incomingIds = currentCertifications.stream()
//                .map(PreOnboardingCertificationDTO::getId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        List<PreOnboardingCertificationEntity> entitiesToDelete = savedCertifications.stream()
//                .filter(saved ->
//                        !incomingIds.contains(saved.getId()))
//                .toList();
//
//        for (PreOnboardingCertificationDTO dto : currentCertifications) {
//            PreOnboardingCertificationEntity entity;
//            if (dto.getId() == null) {
//                entity = preOnboardingCertificationMapper.toEntity(dto);
//                entity.setPreOnboardingId(preOnboardingEntity.getId());
//
//            } else {
//                entity = savedCertifications.stream()
//                        .filter(saved ->
//                                saved.getId().equals(dto.getId()))
//                        .findFirst()
//                        .orElseThrow(() ->
//                                new ManualValidationException(
//                                        "Target certification record matching ID not found"));
//
//                preOnboardingCertificationMapper.updateEntityFromDto(dto, entity);
//            }
//            entitiesToSave.add(entity);
//        }
//        if (!entitiesToDelete.isEmpty()) {
//            preOnboardingCertificationRepository.deleteAllInBatch(entitiesToDelete);
//        }
//
//        if (!entitiesToSave.isEmpty()) {
//            preOnboardingCertificationRepository.saveAll(entitiesToSave);
//        }
//    }

    public PreOnboardingDocumentDTO uploadPreOnboardingFile(UUID applicationId, PreOnboardingDocumentType documentType, MultipartFile file) {
        Optional<PreOnboardingEntity> preOnboardingEntityOpt=preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity = preOnboardingEntityOpt.get();
        String referenceTable = PreOnboardingOtherDetailsEntity.ENTITY_TYPE;
        //Get file extension
        String fileExtension = fileValidationUtil.getExtensionFromMimeType(file.getContentType());
        // Give file name
        String fileName= "pre-onboarding"+documentType+UUID.randomUUID()+"."+fileExtension;
        String uploadedFileName="pre-onboarding"+documentType+UUID.randomUUID()+"."+fileExtension;
        String documentUrl=null;
        try{
            documentUrl=canDocUploadPath+"/"+fileService.uploadFile(file,uploadedFileName,canDocUploadPath);
        }catch (Exception e){
            throw new ManualValidationException("File upload failed: "+e.getMessage());
        }
        PreOnboardingDocumentEntity preOnboardingDocument = PreOnboardingDocumentEntity.builder()
                .preOnboardingId(preOnboardingEntity.getId())
                .referenceTable(referenceTable)
                .referenceId(null)
                .documentType(documentType)
                .documentName(fileName)
                .documentUrl(documentUrl)
                .build();
        return preOnboardingDocumentMapper.toDTO(preOnboardingDocument);
    }
}
