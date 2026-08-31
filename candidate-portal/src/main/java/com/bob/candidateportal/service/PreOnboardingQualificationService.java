package com.bob.candidateportal.service;

import com.bob.candidateportal.model.PreOnboardingQualificationModel;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.db.dto.PreOnboardingCertificationDTO;
import com.bob.db.dto.PreOnboardingFamilyDetailsDTO;
import com.bob.db.dto.PreOnboardingGratuityNominationDTO;
import com.bob.db.dto.PreOnboardingQualificationDTO;
import com.bob.db.entity.*;
import com.bob.db.mapper.PreOnboardingCertificationMapper;
import com.bob.db.mapper.PreOnboardingFamilyDetailsMapper;
import com.bob.db.mapper.PreOnboardingGratuityNominationMapper;
import com.bob.db.mapper.PreOnboardingQualificationMapper;
import com.bob.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PreOnboardingQualificationService {

    @Autowired
    private PreOnboardingFamilyDetailsRepository familyDetailsRepository;

    @Autowired
    private PreOnboardingDocumentRepository preOnboardingDocumentRepository;

    @Autowired
    private PreOnboardingGratuityNominationRepository preOnboardingGratuityNominationRepository;

    @Autowired
    private PreOnboardingCertificationRepository preOnboardingCertificationRepository;

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;

    @Autowired
    private PreOnboardingQualificationRepository preOnboardingQualificationRepository;

    @Autowired
    private PreOnboardingQualificationMapper preOnboardingQualificationMapper;

    @Autowired
    private PreOnboardingFamilyDetailsMapper preOnboardingFamilyDetailsMapper;

    @Autowired
    private PreOnboardingCertificationMapper preOnboardingCertificationMapper;

    @Autowired
    private PreOnboardingGratuityNominationMapper preOnboardingGratuityNominationMapper;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;


    public PreOnboardingQualificationModel getQualDetailsByAppId(UUID applicationId) {

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
        List<PreOnboardingQualificationEntity> preOnboardingQualificationEntityList=preOnboardingQualificationRepository
                .findByPreOnboardingId(preOnboardingEntity.getId());
        List<PreOnboardingCertificationEntity> preOnboardingCertificationEntities=preOnboardingCertificationRepository
                .findByPreOnboardingId(preOnboardingEntity.getId());
        Optional<PreOnboardingFamilyDetailsEntity> preOnboardingFamilyDetailsOpt=familyDetailsRepository
                .findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingFamilyDetailsEntity preOnboardingFamilyDetails=null;
        if(preOnboardingFamilyDetailsOpt.isPresent()){
            preOnboardingFamilyDetails=preOnboardingFamilyDetailsOpt.get();
        }
        Optional<PreOnboardingGratuityNominationEntity> nominationEntitiesOpt=preOnboardingGratuityNominationRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingGratuityNominationEntity nominationEntity=null;
        if(nominationEntitiesOpt.isPresent()){
            nominationEntity=nominationEntitiesOpt.get();
        }
        return PreOnboardingQualificationModel.builder()
                .candidateParentModel(candidateParentModel)
                .preOnboardingCertifications(preOnboardingCertificationMapper.toDTOList(preOnboardingCertificationEntities))
                .preOnboardingQualifications(preOnboardingQualificationMapper.toDTOList(preOnboardingQualificationEntityList))
                .preOnboardingFamilyDetails(preOnboardingFamilyDetailsMapper.toDTO(preOnboardingFamilyDetails))
                .preOnboardingGratuityNomination(preOnboardingGratuityNominationMapper.toDTO(nominationEntity))
                .build();

    }

    public void saveOrUpdateQualDetails(UUID applicationId,PreOnboardingQualificationModel qualificationModel){
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
        //Save or update qualification details
        saveOrUpdateQualificationDetails(qualificationModel,preOnboardingEntity);
        //Save or update certification details
        saveOrUpdateCertificationDetails(qualificationModel,preOnboardingEntity);
        //Save or update family details
        saveOrUpdateFamilyDetails(qualificationModel,preOnboardingEntity);
        //Save or update Gratuity details
        saveOrUpdateGratuityDetails(qualificationModel,preOnboardingEntity);
        if(candidateApplications.getStepper()==3){
            candidateApplications.setStepper(candidateApplications.getStepper()+1);
            candidateApplicationsRepository.save(candidateApplications);
        }
    }

    private void saveOrUpdateQualificationDetails(PreOnboardingQualificationModel qualificationModel,PreOnboardingEntity preOnboardingEntity){
        List<PreOnboardingQualificationEntity> savedQualifications=preOnboardingQualificationRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        //Delete existing data
        preOnboardingQualificationRepository.deleteAll(savedQualifications);
        //Save new data
        List<PreOnboardingQualificationEntity> newQualifications=preOnboardingQualificationMapper.toEntityList(qualificationModel.getPreOnboardingQualifications());
        newQualifications.forEach(nQ->nQ.setPreOnboardingId(preOnboardingEntity.getId()));
        preOnboardingQualificationRepository.saveAll(newQualifications);
    }

    private void saveOrUpdateFamilyDetails(PreOnboardingQualificationModel qualificationModel, PreOnboardingEntity preOnboardingEntity) {
        PreOnboardingFamilyDetailsDTO dto = qualificationModel.getPreOnboardingFamilyDetails();
        if (dto == null) {
            return;
        }
        Optional<PreOnboardingFamilyDetailsEntity> existingFamilyDetailsOpt = familyDetailsRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingFamilyDetailsEntity familyDetailsEntity;
        if (existingFamilyDetailsOpt.isPresent()) {
            familyDetailsEntity = existingFamilyDetailsOpt.get();
            preOnboardingFamilyDetailsMapper.updateEntityFromDto(dto, familyDetailsEntity);
        } else {
            familyDetailsEntity = preOnboardingFamilyDetailsMapper.toEntity(dto);
            familyDetailsEntity.setPreOnboardingId(preOnboardingEntity.getId());
        }
        familyDetailsRepository.save(familyDetailsEntity);
    }

    private void saveOrUpdateCertificationDetails(PreOnboardingQualificationModel qualificationModel, PreOnboardingEntity preOnboardingEntity) {
        List<PreOnboardingCertificationEntity> savedCertifications = preOnboardingCertificationRepository
                        .findByPreOnboardingId(preOnboardingEntity.getId());
        preOnboardingCertificationRepository.deleteAll(savedCertifications);
        List<PreOnboardingCertificationEntity> newCertifications= preOnboardingCertificationMapper.toEntityList(qualificationModel.getPreOnboardingCertifications());
        newCertifications.forEach(nQ->nQ.setPreOnboardingId(preOnboardingEntity.getId()));
        preOnboardingCertificationRepository.saveAll(newCertifications);

    }
    private void saveOrUpdateGratuityDetails(PreOnboardingQualificationModel qualificationModel, PreOnboardingEntity preOnboardingEntity) {
        PreOnboardingGratuityNominationDTO dto = qualificationModel.getPreOnboardingGratuityNomination();
        if (dto == null) {
            return;
        }
        Optional<PreOnboardingGratuityNominationEntity> existingGratuityOpt = preOnboardingGratuityNominationRepository.findByPreOnboardingId(preOnboardingEntity.getId());
        PreOnboardingGratuityNominationEntity gratuityEntity;
        if (existingGratuityOpt.isPresent()) {
            gratuityEntity = existingGratuityOpt.get();
            preOnboardingGratuityNominationMapper.updateEntityFromDto(dto, gratuityEntity);
        } else {
            gratuityEntity =
                    preOnboardingGratuityNominationMapper.toEntity(dto);
            gratuityEntity.setPreOnboardingId(preOnboardingEntity.getId());
        }

        preOnboardingGratuityNominationRepository.save(gratuityEntity);
    }


}
