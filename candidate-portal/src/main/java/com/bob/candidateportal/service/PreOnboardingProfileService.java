package com.bob.candidateportal.service;

import com.bob.candidateportal.model.PreOnboardingProfileModel;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.FileValidationUtil;
import com.bob.commonutil.util.SecurityUtils;

import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.PreOnboardingDocumentType;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class PreOnboardingProfileService {

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;

    @Autowired
    private PreOnboardingPersonalDetailsRepository personalDetailsRepository;

    @Autowired
    private PreOnboardingAddressRepository addressRepository;

    @Autowired
    private PreOnboardingDomicileDetailsRepository domicileDetailsRepository;

    //Add mappers
    @Autowired
    private PreOnboardingPersonalDetailsMapper personalDetailsMapper;

    @Autowired
    private PreOnboardingDomicileDetailsMapper domicileDetailsMapper;

    @Autowired
    private PreOnboardingAddressMapper addressMapper;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private FileService fileService;

    private FileValidationUtil fileValidationUtil;

    @Value("${candidate.document.upload.path}")
    private String canDocUploadPath;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private PreOnboardingDocumentRepository preOnboardingDocumentRepository;

    @Autowired
    private PreOnboardingDocumentMapper preOnboardingDocumentMapper;

    @Autowired
    private PreOnboardingMapper preOnboardingMapper;

    @Autowired
    private PreOnboardingDisabilityDetailsRepository disabilityDetailsRepository;

    @Autowired
    private PreOnboardingDisabilityDetailsMapper disabilityDetailsMapper;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;


    @Transactional
    public PreOnboardingProfileModel getPreOnboardingDetailsByAppId(UUID applicationId) {
        Optional<CandidateApplicationsEntity> candidateApplicationsEntityOpt=candidateApplicationsRepository.findById(applicationId);
        if(!candidateApplicationsEntityOpt.isPresent()){
            throw new ManualValidationException("Application not found");
        }
        CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityOpt.get();
        CandidateParentModel candidateParentModel=candidateCommonGetService.getCandidateBasicDetails(candidateApplications.getCandidateId(),candidateApplications.getPositionId());
        Optional<PreOnboardingEntity> preOnboardingEntityOpt = preOnboardingRepository.findByApplicationId(applicationId);
        if (!preOnboardingEntityOpt.isPresent()) {
            return null;
        }
        PreOnboardingEntity preOnboardingEntity = preOnboardingEntityOpt.get();
        PreOnboardingPersonalDetailsEntity personalDetails = personalDetailsRepository.findByPreOnboardingId(preOnboardingEntity.getId()).orElse(null);
        PreOnboardingAddressEntity addressEntity = addressRepository.findByPreOnboardingId(preOnboardingEntity.getId()).orElse(null);
        PreOnboardingDomicileDetailsEntity domicileDetailsEntity = domicileDetailsRepository.findByPreOnboardingId(preOnboardingEntity.getId()).orElse(null);
        List<String> referenceTables = Arrays.asList(PreOnboardingAddressEntity.ENTITY_TYPE, PreOnboardingDomicileDetailsEntity.ENTITY_TYPE);
        List<PreOnboardingDocumentEntity> documentEntities = preOnboardingDocumentRepository.findByPreOnboardingIdAndReferenceTableIn(preOnboardingEntity.getId(), referenceTables);
        List<PreOnboardingDisabilityDetailsEntity> preOnboardingDisabilityDetailsEntities=disabilityDetailsRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingProfileModel profileModel = PreOnboardingProfileModel.builder()
                .candidateParentModel(candidateParentModel)
                .personalDetails(personalDetailsMapper.toDTO(personalDetails))
                .address(addressMapper.toDTO(addressEntity))
                .domicileDetails(domicileDetailsMapper.toDTO(domicileDetailsEntity))
                .disabilityDetails(disabilityDetailsMapper.toDTOList(preOnboardingDisabilityDetailsEntities))
                .documents(preOnboardingDocumentMapper.toDTOList(documentEntities))
                .build();
        return profileModel;
    }

    @Transactional
    public void saveOrUpdatePreOnboardingProfile(UUID applicationId, PreOnboardingProfileModel profileModel) throws IOException {
        UUID candidateId = securityUtils.getCurrentUserId();
        Optional<CandidateApplicationsEntity> candidateApplicationsEntityOpt=candidateApplicationsRepository.findById(applicationId);
        if(!candidateApplicationsEntityOpt.isPresent()){
            throw new ManualValidationException("Application not found");
        }
        CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityOpt.get();
        Optional<PreOnboardingEntity> preOnboardingEntityOpt=preOnboardingRepository.findByApplicationId(applicationId);
        PreOnboardingEntity savedPreOnboardingEntity = null;
        PreOnboardingAddressEntity addressEntity=null;
        PreOnboardingPersonalDetailsEntity personalDetailsEntity=null;
        PreOnboardingDomicileDetailsEntity domicileDetailsEntity=null;
        if(!preOnboardingEntityOpt.isPresent()){
            PreOnboardingEntity preOnboardingEntity = PreOnboardingEntity.builder()
                    .applicationId(applicationId)
                    .candidateId(candidateId)
                    .build();
            savedPreOnboardingEntity = preOnboardingRepository.save(preOnboardingEntity);
        }
        else{
            savedPreOnboardingEntity=preOnboardingEntityOpt.get();
        }
        // Get the preOnboarding entity id
        UUID preOnboardingId=savedPreOnboardingEntity.getId();
        //Save or update personal details
        Optional<PreOnboardingPersonalDetailsEntity> personalDetailsEntityOpt = personalDetailsRepository.findByPreOnboardingId(preOnboardingId);
        if (!personalDetailsEntityOpt.isPresent()) {
            personalDetailsEntity = personalDetailsMapper.toEntity(profileModel.getPersonalDetails());
            personalDetailsEntity.setPreOnboardingId(preOnboardingId);
        }
        else{
            personalDetailsEntity = personalDetailsEntityOpt.get();
            personalDetailsMapper.updateEntityFromDto(profileModel.getPersonalDetails(), personalDetailsEntity);
        }
        //Save or update address details
        Optional<PreOnboardingAddressEntity> addressEntityOpt = addressRepository.findByPreOnboardingId(preOnboardingId);
        if (!addressEntityOpt.isPresent()) {
            addressEntity = addressMapper.toEntity(profileModel.getAddress());
            addressEntity.setPreOnboardingId(preOnboardingId);
        }
        else{
            addressEntity = addressEntityOpt.get();
            addressMapper.updateEntityFromDto(profileModel.getAddress(), addressEntity);
        }
        //Save or update domicile details
        Optional<PreOnboardingDomicileDetailsEntity> domicileDetailsEntityOpt = domicileDetailsRepository.findByPreOnboardingId(preOnboardingId);
        if (!domicileDetailsEntityOpt.isPresent()) {
            domicileDetailsEntity = domicileDetailsMapper.toEntity(profileModel.getDomicileDetails());
            domicileDetailsEntity.setPreOnboardingId(preOnboardingId);
        }
        else {
            domicileDetailsEntity = domicileDetailsEntityOpt.get();
            domicileDetailsMapper.updateEntityFromDto(profileModel.getDomicileDetails(), domicileDetailsEntity);
        }
        //Save disability details
        List<PreOnboardingDisabilityDetailsEntity> oldPreOnbEntities=disabilityDetailsRepository.findByPreOnboardingId(preOnboardingId);
        disabilityDetailsRepository.deleteAll(oldPreOnbEntities);
        List<PreOnboardingDisabilityDetailsEntity> newPreOnbEntities=disabilityDetailsMapper.toEntityList(profileModel.getDisabilityDetails());
        newPreOnbEntities.stream().forEach(preOnboardingDisabilityDetailsEntity -> preOnboardingDisabilityDetailsEntity.setPreOnboardingId(preOnboardingId));
        // Save all entities(personal details, address and domicile details)
        personalDetailsRepository.save(personalDetailsEntity);
        addressRepository.save(addressEntity);
        domicileDetailsRepository.save(domicileDetailsEntity);
        disabilityDetailsRepository.saveAll(newPreOnbEntities);
        if(candidateApplications.getStepper()==1){
            candidateApplications.setStepper(candidateApplications.getStepper()+1);
            candidateApplicationsRepository.save(candidateApplications);
        }
    }




    private String resolveReferenceTable(PreOnboardingDocumentType docType) {
        return switch (docType) {
            case ADDRESS_PROOF                      -> PreOnboardingAddressEntity.ENTITY_TYPE;
            case VOTER_ID, DRIVING_LICENSE,
                 PAN_CARD, PASSPORT                -> PreOnboardingDomicileDetailsEntity.ENTITY_TYPE;
            default                                -> PreOnboardingDocumentEntity.ENTITY_TYPE;
        };
    }


    public PreOnboardingDocumentDTO uploadPreOnboardingFile(UUID applicationId, PreOnboardingDocumentType documentType, MultipartFile file) {
        Optional<PreOnboardingEntity> preOnboardingEntityOpt = preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity = preOnboardingEntityOpt.get();
        String referenceTable = resolveReferenceTable(documentType);
        //Get file extension
        String fileExtension = fileValidationUtil.getExtensionFromMimeType(file.getContentType());
        // Give file name
        String fileName= "pre-onboarding_"+documentType;
        String uploadedFileName="pre-onboarding_"+documentType+"_"+UUID.randomUUID()+"."+fileExtension;
        String documentUrl=null;
        try{
            documentUrl=canDocUploadPath+"/"+fileService.uploadFile(file,uploadedFileName,canDocUploadPath);
        }catch (Exception e){
            throw new ManualValidationException("File upload failed: "+e.getMessage());
        }
        //Check any existing document for the same reference and delete the file and record
        List<PreOnboardingDocumentEntity> existingDocuments = preOnboardingDocumentRepository.findByPreOnboardingIdAndReferenceTableIn(preOnboardingEntity.getId(), List.of(referenceTable));
        if(existingDocuments.size()>1){
            throw new ManualValidationException("Multiple documents found for the same reference.Can't update the document");
        }
        PreOnboardingDocumentEntity preOnboardingDocument=null;
        if(!existingDocuments.isEmpty()) preOnboardingDocument = existingDocuments.get(0);
        if(preOnboardingDocument!=null) {
            //Delete existing file
            if (preOnboardingDocument.getDocumentUrl() != null) {
                String blobName = preOnboardingDocument.getDocumentUrl()
                        .substring(preOnboardingDocument.getDocumentUrl().lastIndexOf("/") + 1);
                fileService.deleteExistingFiles(canDocUploadPath, blobName);
            }
            //Update document record with new file details
            preOnboardingDocument.setDocumentName(fileName);
            preOnboardingDocument.setDocumentUrl(documentUrl);

        }
        else{
            //Create new document record
            preOnboardingDocument = PreOnboardingDocumentEntity.builder()
                    .preOnboardingId(preOnboardingEntity.getId())
                    .referenceTable(referenceTable)
                    .referenceId(null)
                    .documentType(documentType)
                    .documentName(fileName)
                    .documentUrl(documentUrl)
                    .build();
        }
        return preOnboardingDocumentMapper.toDTO(preOnboardingDocumentRepository.save(preOnboardingDocument));
    }


}
