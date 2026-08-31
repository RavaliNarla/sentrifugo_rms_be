package com.bob.candidateportal.service;

import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.CandidateCommonGetService;
import com.bob.db.dto.CandidateAddressDTO;
import com.bob.db.entity.CandidateAddressEntity;
import com.bob.db.entity.CandidatesEntity;
import com.bob.db.mapper.CandidateAddressMapper;
import com.bob.db.repository.CandidateAddressRepository;
import com.bob.db.repository.CandidatesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CandidateAddressService {

    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private CandidateAddressMapper candidateAddressMapper;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;

    public CandidateAddressDTO saveCandidateAddress(UUID candidateId, CandidateAddressDTO candidateAddress) {

       candidateValidationUtil.validateCandidateExistence(candidateId);
        Optional<CandidateAddressEntity> optional =
                candidateAddressRepository.findByCandidateId(candidateId);
        CandidateAddressEntity savedAddress;
        if (optional.isPresent()) {
            CandidateAddressEntity existingEntity = optional.get();
            candidateAddressMapper.updateEntityFromDto(candidateAddress, existingEntity);
            savedAddress = candidateAddressRepository.save(existingEntity);
        }else {

            CandidatesEntity candidateEntity = candidatesRepository.findById(candidateId).orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
            candidateEntity.setCurrentStep(Short.parseShort("5"));
            candidateAddress.setCandidateId(candidateId);
            savedAddress = candidateAddressRepository.save( candidateAddressMapper.toEntity(candidateAddress));
        }
        return  candidateAddressMapper.toDTO( savedAddress);
    }

    public CandidateAddressDTO getCandidateAddress(UUID candidateId) {
       return candidateCommonGetService.getCandidateAddress(candidateId);
    }
}
