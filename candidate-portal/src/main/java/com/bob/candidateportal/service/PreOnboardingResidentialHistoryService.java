package com.bob.candidateportal.service;

import com.bob.candidateportal.model.PreOnboardingPrevOrgDetailsModel;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.FileValidationUtil;
import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.dto.PreOnboardingResidentialHistoryDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.PreOnboardingDocumentType;
import com.bob.db.mapper.PreOnboardingDocumentMapper;
import com.bob.db.mapper.PreOnboardingPreviousOrganisationMapper;
import com.bob.db.mapper.PreOnboardingResidentialHistoryMapper;
import com.bob.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PreOnboardingResidentialHistoryService {

    @Autowired
    private PreOnboardingPreviousOrganisationRepository previousOrganisationRepository;

    @Autowired
    private PreOnboardingPreviousOrganisationMapper previousOrganisationMapper;

    @Autowired
    private PreOnboardingResidentialHistoryMapper residentialHistoryMapper;

    @Autowired
    private PreOnboardingResidentialHistoryRepository residentialHistoryRepository;

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;
    @Autowired
    private PreOnboardingDocumentRepository preOnboardingDocumentRepository;

    @Autowired
    private PreOnboardingDocumentMapper preOnboardingDocumentMapper;

    @Autowired
    private PreOnboardingFamilyDetailsRepository familyDetailsRepository;

    private FileValidationUtil fileValidationUtil;

    @Value("${candidate.document.upload.path}")
    private String canDocUploadPath;

    @Autowired
    private FileService fileService;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    public PreOnboardingPrevOrgDetailsModel getPreviousEmploymentDetails(UUID applicationId) {
        Optional<CandidateApplicationsEntity> candidateApplicationsEntityOpt=candidateApplicationsRepository.findById(applicationId);
        if(!candidateApplicationsEntityOpt.isPresent()){
            throw new ManualValidationException("Application not found");
        }
        CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityOpt.get();
        CandidateParentModel candidateParentModel=candidateCommonGetService.getCandidateBasicDetails(candidateApplications.getCandidateId(),candidateApplications.getPositionId());

        Optional<PreOnboardingEntity> preOnboardingEntityOpt = preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity = preOnboardingEntityOpt.get();
        List<PreOnboardingPreviousOrganisationEntity> previousOrganisations=previousOrganisationRepository.findByPreOnboardingId(preOnboardingEntity.getId());

        List<PreOnboardingResidentialHistoryEntity> residentialHistoryEntities = residentialHistoryRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        List<UUID> resentOrganisationIds = residentialHistoryEntities.stream()
                .map(PreOnboardingResidentialHistoryEntity::getId)
                .collect(Collectors.toList());
        List<UUID> referenceIds = new ArrayList<>();
        referenceIds.addAll(resentOrganisationIds);
        List<PreOnboardingDocumentEntity> documentEntities = preOnboardingDocumentRepository.
                findByPreOnboardingIdAndReferenceIdIn(preOnboardingEntity.getId(),referenceIds);
        Map<UUID,PreOnboardingDocumentEntity> preOnboardingDocumentEntityMap=documentEntities.stream()
                .collect(Collectors.toMap(PreOnboardingDocumentEntity::getReferenceId,preOnboardingDocumentEntity -> preOnboardingDocumentEntity));
        List<PreOnboardingResidentialHistoryDTO> residentialHistoryDTOS=residentialHistoryMapper.toDTOList(residentialHistoryEntities);
        for(PreOnboardingResidentialHistoryDTO dto: residentialHistoryDTOS){
            UUID referenceId=dto.getId();
            PreOnboardingDocumentEntity preOnboardingDocument=preOnboardingDocumentEntityMap.get(referenceId);
            if(preOnboardingDocument!=null) dto.setPreOnboardingDocumentId(preOnboardingDocument.getId());
        }
        PreOnboardingPrevOrgDetailsModel prevOrgDetailsModel=PreOnboardingPrevOrgDetailsModel.builder()
                .candidateParentModel(candidateParentModel)
                .previousOrganisations(previousOrganisationMapper.toDTOList(previousOrganisations))
                .residentialHistory(residentialHistoryDTOS)
                .documents(preOnboardingDocumentMapper.toDTOList(documentEntities))
                .build();
        return prevOrgDetailsModel;

    }


    @Transactional
    public void saveOrUpdatePrevOrgDetails(UUID applicationId, PreOnboardingPrevOrgDetailsModel profileModel) {
        Optional<CandidateApplicationsEntity> candidateApplicationsEntityOpt=candidateApplicationsRepository.findById(applicationId);
        if(!candidateApplicationsEntityOpt.isPresent()){
            throw new ManualValidationException("Application not found");
        }
        CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityOpt.get();
        Optional<PreOnboardingEntity> preOnboardingEntityOpt = preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity = preOnboardingEntityOpt.get();
        List<PreOnboardingResidentialHistoryEntity> existingHistories =
                residentialHistoryRepository.findByPreOnboardingId(preOnboardingEntity.getId());

        Map<UUID, PreOnboardingResidentialHistoryEntity> existingHistoryMap = existingHistories
                .stream()
                .collect(Collectors.toMap(PreOnboardingResidentialHistoryEntity::getId, Function.identity()));

        List<PreOnboardingDocumentEntity> preOnboardingDocs = preOnboardingDocumentRepository
                .findByPreOnboardingIdAndReferenceTableIn(
                        preOnboardingEntity.getId(),
                        List.of(PreOnboardingResidentialHistoryEntity.ENTITY_TYPE)
                );
        //Save or update the previous organization data
        List<PreOnboardingPreviousOrganisationEntity> oldOrganisations=previousOrganisationRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        List<PreOnboardingPreviousOrganisationEntity> newOrganisations=previousOrganisationMapper.toEntityList(profileModel.getPreviousOrganisations());
        newOrganisations.forEach(nO->nO.setPreOnboardingId(preOnboardingEntity.getId()));
        previousOrganisationRepository.deleteAll(oldOrganisations);
        previousOrganisationRepository.saveAll(newOrganisations);
        Map<UUID, PreOnboardingDocumentEntity> existingDocsByRefId =
                preOnboardingDocs.stream()
                        .filter(doc -> doc.getReferenceId() != null)
                        .collect(Collectors.toMap(
                                PreOnboardingDocumentEntity::getReferenceId,
                                Function.identity()
                        ));

        Map<UUID, PreOnboardingDocumentEntity> existingDocsByDocId = preOnboardingDocs.stream()
                .collect(Collectors.toMap(PreOnboardingDocumentEntity::getId, doc -> doc));

        Set<UUID> incomingIds=profileModel.getResidentialHistory().stream()
                .map(PreOnboardingResidentialHistoryDTO::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<PreOnboardingResidentialHistoryEntity> historyEntitiesToDelete=new ArrayList<>();
        List<PreOnboardingDocumentEntity> docEntitiesToDelete=new ArrayList<>();
        List<PreOnboardingResidentialHistoryEntity> historyEntitiesToSave=new ArrayList<>();
        List<PreOnboardingDocumentEntity> docEntitiesToSave=new ArrayList<>();

        // Get updated data
        for(Map.Entry<UUID, PreOnboardingResidentialHistoryEntity> entityEntry:existingHistoryMap.entrySet()){
            if(!incomingIds.contains(entityEntry.getKey())){
                historyEntitiesToDelete.add(entityEntry.getValue());
                PreOnboardingDocumentEntity documentEntity=existingDocsByRefId.get(entityEntry.getKey());
                if(documentEntity!=null){
                    docEntitiesToDelete.add(documentEntity);
                }
            }
        }

        for(PreOnboardingResidentialHistoryDTO dto: profileModel.getResidentialHistory()){
            PreOnboardingResidentialHistoryEntity currEntity;
            if(dto.getId()==null){
                currEntity=residentialHistoryMapper.toEntity(dto);
                PreOnboardingDocumentEntity preOnboardingDocument=existingDocsByDocId.get(dto.getPreOnboardingDocumentId());
                currEntity.setPreOnboardingId(preOnboardingEntity.getId());
            }
            else{
                currEntity=existingHistoryMap.get(dto.getId());
                if(currEntity==null) throw new ManualValidationException("Residential History not found");
                residentialHistoryMapper.updateEntityFromDto(dto,currEntity);
            }
            historyEntitiesToSave.add(currEntity);
        }
        // Delete the history data
        if(!historyEntitiesToDelete.isEmpty()){
            residentialHistoryRepository.deleteAll(historyEntitiesToDelete);
        }
        //Save the updated history data
        List<PreOnboardingResidentialHistoryEntity> savedHistories=residentialHistoryRepository.saveAll(historyEntitiesToSave);
        //Now delete the documents
        for(int i=0;i<savedHistories.size();i++){
            PreOnboardingResidentialHistoryDTO currDto=profileModel.getResidentialHistory().get(i);
            PreOnboardingResidentialHistoryEntity currEntity=savedHistories.get(i);
            if(currDto.getPreOnboardingDocumentId()==null){
                PreOnboardingDocumentEntity existingDoc =
                        existingDocsByRefId.get(currEntity.getId());

                if(existingDoc != null) {
                    docEntitiesToDelete.add(existingDoc);
                }
                continue;
            }
            PreOnboardingDocumentEntity currDocEntity=existingDocsByDocId.get(currDto.getPreOnboardingDocumentId());
            if(currDocEntity==null){
                throw new ManualValidationException("Document not found for current history!");
            }
            PreOnboardingDocumentEntity existingDocEntity=existingDocsByRefId.get(currEntity.getId());
            if (existingDocEntity != null && !currDocEntity.getId().equals(existingDocEntity.getId())) {
                docEntitiesToDelete.add(existingDocEntity);
            }
            currDocEntity.setReferenceTable(PreOnboardingResidentialHistoryEntity.ENTITY_TYPE);
            currDocEntity.setReferenceId(currEntity.getId());
            docEntitiesToSave.add(currDocEntity);
        }
        // Delete all the old files and save new data
        if(!docEntitiesToDelete.isEmpty()){
            preOnboardingDocumentRepository.deleteAll(docEntitiesToDelete);
        }
        //Save the new records
        if(!docEntitiesToSave.isEmpty()){
            preOnboardingDocumentRepository.saveAll(docEntitiesToSave);
        }
        // clean up the documents whose reference Id is null
//        List<PreOnboardingDocumentEntity> documentsToBeDeleted=preOnboardingDocumentRepository.findByPreOnboardingId(preOnboardingEntity.getId())
//                .stream().filter(preOnboardingDocumentEntity -> preOnboardingDocumentEntity.getReferenceId()==null).toList();
//        preOnboardingDocumentRepository.deleteAll(documentsToBeDeleted);
        if(candidateApplications.getStepper()==2){
            candidateApplications.setStepper(candidateApplications.getStepper()+1);
            candidateApplicationsRepository.save(candidateApplications);
        }
    }

    public PreOnboardingDocumentDTO uploadPreOnboardingFile(UUID applicationId, PreOnboardingDocumentType documentType, MultipartFile file) {
        Optional<PreOnboardingEntity> preOnboardingEntityOpt = preOnboardingRepository.findByApplicationId(applicationId);
        if(!preOnboardingEntityOpt.isPresent()){
            throw new ManualValidationException("Pre-onboarding details not found for the given application");
        }
        PreOnboardingEntity preOnboardingEntity = preOnboardingEntityOpt.get();
        String referenceTable = PreOnboardingResidentialHistoryEntity.ENTITY_TYPE;
        //Get file extension
        String fileExtension = fileValidationUtil.getExtensionFromMimeType(file.getContentType());
        // Give file name
        String fileName= "pre-onboarding_"+documentType+"_"+UUID.randomUUID()+"."+fileExtension;
        String uploadedFileName="pre-onboarding_"+documentType+"_"+UUID.randomUUID()+"."+fileExtension;
        String documentUrl=null;
        try{
            documentUrl= canDocUploadPath+"/"+fileService.uploadFile(file,uploadedFileName,canDocUploadPath);
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
        return preOnboardingDocumentMapper.toDTO(preOnboardingDocumentRepository.save(preOnboardingDocument));
    }
}
