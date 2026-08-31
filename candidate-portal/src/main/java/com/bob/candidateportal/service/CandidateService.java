package com.bob.candidateportal.service;


import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.CandidateParentModel;

import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class CandidateService {

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private CandidateExperienceService candidateExperienceService;

    @Autowired
    private CandidateBasicDetailsService candidateBasicDetailsService;

    @Autowired
    private CandidateAddressService candidateAddressService;

    @Autowired
    private CandidateEducationService candidateEducationService;

    @Autowired
    private CandidateDocumentsService candidateDocumentsService;
    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;


    public CandidateParentModel getCandidateBasicDetails(UUID candidateId, UUID positionId){
        return candidateCommonGetService.getCandidateBasicDetails(candidateId,positionId);
    }

    public Boolean saveCandidateProfileComplete(UUID candidateId, Boolean isProfileCompleted) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        CandidatesEntity entity = candidatesRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found."));
        entity.setIsProfileCompleted(isProfileCompleted);
        CandidatesEntity saved = candidatesRepository.save(entity);
        return saved.getIsProfileCompleted();
    }
}
