package com.bob.commonutil.util;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.repository.CandidatesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CandidateValidationUtil {

   private final CandidatesRepository candidatesRepository;

    @Transactional(readOnly = true)
    public void validateCandidateExistence(UUID candidateId){
        if (candidateId == null) {
            throw new IllegalArgumentException("Select a candidate");
        }

        if(!candidatesRepository.existsById(candidateId)){
            throw new ResourceNotFoundException("Candidate not found.");
        }
    }
}
