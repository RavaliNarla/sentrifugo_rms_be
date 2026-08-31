package com.bob.candidateportal.service;

import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.PreOnboardingDTO;
import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.PreOnboardingDocumentEntity;
import com.bob.db.entity.PreOnboardingEntity;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.mapper.PreOnboardingDocumentMapper;
import com.bob.db.mapper.PreOnboardingMapper;
import com.bob.db.repository.CandidateApplicationsRepository;
import com.bob.db.repository.PreOnboardingDocumentRepository;
import com.bob.db.repository.PreOnboardingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Optional;
import java.util.UUID;

@Service
public class PreOnboardingService {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private PreOnboardingMapper preOnboardingMapper;

    @Autowired
    private PreOnboardingDocumentRepository preOnboardingDocumentRepository;

    @Autowired
    private PreOnboardingDocumentMapper preOnboardingDocMapper;

    @Autowired
    private FileService fileService;

    @Value("${candidate.document.upload.path}")
    private String canDocUploadPath;

    public PreOnboardingDTO savePreOnboarding(UUID applicationId) {
        UUID candidateId=securityUtils.getCurrentUserId();
        Optional<PreOnboardingEntity> preOnboardingEntityOpt = preOnboardingRepository.findByApplicationId(applicationId);
        PreOnboardingEntity preOnboardingEntity =null;
        if(!preOnboardingEntityOpt.isPresent()){
            preOnboardingEntity = PreOnboardingEntity.builder()
                    .applicationId(applicationId)
                    .candidateId(candidateId)
                    .build();

        }else{
            preOnboardingEntity=preOnboardingEntityOpt.get();
        }
        PreOnboardingEntity savedOnboarding=preOnboardingRepository.save(preOnboardingEntity);
        Optional<CandidateApplicationsEntity> candidateApplicationsOpt=candidateApplicationsRepository.findById(applicationId);
        CandidateApplicationsEntity candidateApplications=null;
        if(!candidateApplicationsOpt.isPresent()){
            throw new ManualValidationException("Cannot update the application status");
        }
        candidateApplications=candidateApplicationsOpt.get();
        candidateApplications.setApplicationStatus(CandidateApplicationStatus.PRE_ONBOARDING_PENDING);
        if(candidateApplications.getStepper()==null || candidateApplications.getStepper()==0) candidateApplications.setStepper(1);
        candidateApplicationsRepository.save(candidateApplications);
        return preOnboardingMapper.toDTO(savedOnboarding);
    }

    public PreOnboardingDocumentDTO deleteDocumentByDocId(UUID preOnboardingDocId) {
        Optional<PreOnboardingDocumentEntity> preOnboardingDocOpt=preOnboardingDocumentRepository.findById(preOnboardingDocId);
        if(!preOnboardingDocOpt.isPresent()){
            throw new ManualValidationException("No Document found");
        }
        PreOnboardingDocumentEntity preOnboardingDocument=preOnboardingDocOpt.get();
        if(preOnboardingDocument.getDocumentUrl()!=null){
            String blobName = preOnboardingDocument.getDocumentUrl()
                    .substring(preOnboardingDocument.getDocumentUrl().lastIndexOf("/") + 1);
            fileService.deleteExistingFiles(canDocUploadPath, blobName);
        }
        preOnboardingDocumentRepository.delete(preOnboardingDocument);
        return preOnboardingDocMapper.toDTO(preOnboardingDocument);
    }
}
