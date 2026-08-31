package com.bob.candidateportal.service;


import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.BasicDetailsModel;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.db.dto.CandidateProfileDTO;
import com.bob.db.dto.LanguagesKnownDTO;
import com.bob.db.entity.CandidateDisabilityDetailsEntity;
import com.bob.db.entity.CandidateProfileEntity;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.entity.LanguagesKnownEntity;
import com.bob.db.mapper.CandidateDisabilityDetailsMapper;
import com.bob.db.mapper.CandidateProfileMapper;
import com.bob.db.mapper.LanguagesKnownMapper;
import com.bob.db.repository.CandidateDisabilityDetailsRepository;
import com.bob.db.repository.CandidateProfileRepository;
import com.bob.db.repository.CandidatesRepository;
import com.bob.db.repository.LanguagesKnownRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CandidateBasicDetailsService {

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private LanguagesKnownRepository languagesKnownRepository;

    @Autowired
    private CandidateProfileMapper candidateProfileMapper;

    @Autowired
    private LanguagesKnownMapper languagesKnownMapper;

    @Autowired
    private CandidateDisabilityDetailsMapper candidateDisabilityDetailsMapper;

    @Autowired
    private CandidateDisabilityDetailsRepository candidateDisabilityDetailsRepository;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;

    @Transactional
    public BasicDetailsModel createOrUpdateBasicDetails(UUID candidateId,BasicDetailsModel dto) {
        CandidateProfileDTO profileDTO = dto.getCandidateProfile();
        List<LanguagesKnownDTO> languagesKnownDTOS = dto.getLanguagesKnown();

       candidateValidationUtil.validateCandidateExistence(candidateId);

        Optional<CandidateProfileEntity> optional =
                candidateProfileRepository.findByCandidateId(candidateId);

        CandidateProfileEntity entity;

        if (optional.isPresent()) {

            List<LanguagesKnownEntity> existingLanguages =languagesKnownRepository.findByCandidateId(candidateId);
            List<CandidateDisabilityDetailsEntity> existingDisabilities = candidateDisabilityDetailsRepository.findAllByCandidateId(candidateId);
            languagesKnownRepository.deleteAll(existingLanguages);
            candidateDisabilityDetailsRepository.deleteAll(existingDisabilities);
            entity = optional.get();

            candidateProfileMapper.updateEntityFromDto(profileDTO, entity);

        } else {
            CandidatesEntity candidateEntity = candidatesRepository.findById(candidateId).orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
            candidateEntity.setCurrentStep(Short.parseShort("4"));
            entity = candidateProfileMapper.toEntity(profileDTO);
            entity.setCandidateId(candidateId);
        }

        List<LanguagesKnownEntity> languagesKnownEntities =
                languagesKnownMapper.toEntityList(languagesKnownDTOS);

        List<CandidateDisabilityDetailsEntity> savedDis =null;

        if(dto.getCandidateProfile().getDisability()){
            List<CandidateDisabilityDetailsEntity> disabilityEntities =
                    candidateDisabilityDetailsMapper.toEntityList(dto.getDisabilityDetails());
            for (CandidateDisabilityDetailsEntity disabilityEntity : disabilityEntities) {
                disabilityEntity.setCandidateId(candidateId);
            }
            savedDis = candidateDisabilityDetailsRepository.saveAll(disabilityEntities);
        }


        for (LanguagesKnownEntity langEntity : languagesKnownEntities) {
            langEntity.setCandidateId(candidateId);
        }

        List<LanguagesKnownEntity> savedLan = languagesKnownRepository.saveAll(languagesKnownEntities);
        CandidateProfileEntity savedEntity = candidateProfileRepository.save(entity);

        return BasicDetailsModel.builder()
                .candidateProfile(candidateProfileMapper.toDTO(savedEntity))
                .languagesKnown(languagesKnownMapper.toDTOList(savedLan))
                .disabilityDetails(candidateDisabilityDetailsMapper.toDTOList(savedDis))
                .build();
    }

    @Transactional
    public BasicDetailsModel getCandidateProfileById(UUID candidateId) {
        return candidateCommonGetService.getCandidateProfileById(candidateId);
    }

    public void saveFresherStatus(UUID candidateId ,Boolean isFresher) {
        candidateValidationUtil.validateCandidateExistence(candidateId);

        CandidateProfileEntity profileEntity = candidateProfileRepository
                .findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.CANDIDATE_PROFILE_NOT_FOUND_MESSAGE));
        profileEntity.setIsFresher(isFresher);
        candidateProfileRepository.save(profileEntity);
    }

    public Boolean getFresherStatus(UUID candidateId) {
        candidateValidationUtil.validateCandidateExistence(candidateId);

        CandidateProfileEntity profileEntity = candidateProfileRepository
                .findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.CANDIDATE_PROFILE_NOT_FOUND_MESSAGE));
        return profileEntity.getIsFresher();
    }

    public void savePersonalDisclaimer( UUID candidateId,Boolean personalDisclaimer) {
        candidateValidationUtil.validateCandidateExistence(candidateId);

        CandidatesEntity profileEntity = candidatesRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.CANDIDATE_PROFILE_NOT_FOUND_MESSAGE));
        profileEntity.setFinalDeclarationAccepted(personalDisclaimer);
        candidatesRepository.save(profileEntity);
    }

    public Boolean getPersonalDisclaimer(UUID candidateId) {

        CandidatesEntity profileEntity = candidatesRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(AppConstants.CANDIDATE_PROFILE_NOT_FOUND_MESSAGE));
        return profileEntity.getFinalDeclarationAccepted();
    }


}
