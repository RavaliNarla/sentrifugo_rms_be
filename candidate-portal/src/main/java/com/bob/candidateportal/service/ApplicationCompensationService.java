package com.bob.candidateportal.service;

import com.bob.candidateportal.model.ApplicationCompensationModel;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ApplicationCompensationDTO;
import com.bob.db.entity.CandidateCompensationEntity;
import com.bob.db.enums.CompensationStatus;
import com.bob.db.mapper.CandidateCompensationMapper;
import com.bob.db.repository.CandidateApplicationsRepository;
import com.bob.db.repository.CandidateCompensationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ApplicationCompensationService {

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;

    @Autowired
    private CandidateCompensationMapper candidateCompensationMapper;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private FileService fileService;

    @Value("${candidate.document.upload.path}")
    private String CANDIDATE_RESUME_FOLDER ;

    @Value("${resume.http.url}")
    private String CANDIDATE_RESUME_DIR_URL;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private MailSenderHelper mailSenderHelper;


    public void saveCompensationDetails(ApplicationCompensationModel requestModel){
        UUID candidateId=securityUtils.getCurrentUserId();
        UUID applicationId=requestModel.getApplicationId();
        LocalDate today=LocalDate.now();
        CandidateCompensationEntity candidateCompensationEntity=candidateCompensationRepository.findByApplicationIdAndCandidateId(applicationId,candidateId)
                .orElseThrow(()->new ManualValidationException("No compensation details found for the given application ID and candidate ID"));
        if(candidateCompensationEntity.getCompensationStatus().equals(CompensationStatus.SUBMITTED)){
            throw new ManualValidationException("Compensation details have already been submitted for this application");
        }
        if(today.isAfter(candidateCompensationEntity.getSubmitBeforeDate())){
            throw new ManualValidationException("Compensation details submission deadline has passed");
        }
        candidateCompensationEntity.setCurrentCtc(requestModel.getCurrentCtc());
        candidateCompensationEntity.setExpectedCtc(requestModel.getExpectedCtc());
        candidateCompensationEntity.setCompensationStatus(CompensationStatus.SUBMITTED);
        BigDecimal hikePercentage=calculateHikePercentage(requestModel.getCurrentCtc(),requestModel.getExpectedCtc());
        candidateCompensationEntity.setHike(hikePercentage);
        candidateCompensationRepository.save(candidateCompensationEntity);
        mailSenderHelper.senMailToRecruiterAboutCandidateCompensationDetails(candidateCompensationEntity.getId());

    }

    private BigDecimal calculateHikePercentage(BigDecimal currentCtc, BigDecimal expectedCtc) {
        if (currentCtc.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Current CTC cannot be zero");
        }
        return expectedCtc.subtract(currentCtc).divide(currentCtc, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
    }


    public ApplicationCompensationModel getCompensationDetails(UUID applicationId) {
        UUID candidateId=securityUtils.getCurrentUserId();
        CandidateCompensationEntity candidateCompensationEntity=candidateCompensationRepository.findByApplicationIdAndCandidateId(applicationId,candidateId)
                .orElseThrow(()->new IllegalArgumentException("No compensation details found for the given application ID and candidate ID"));
        return ApplicationCompensationModel.builder()
                .applicationId(applicationId)
                .currentCtc(candidateCompensationEntity.getCurrentCtc())
                .expectedCtc(candidateCompensationEntity.getExpectedCtc())
                .submitBeforeDate(candidateCompensationEntity.getSubmitBeforeDate())
                .compensationStatus(candidateCompensationEntity.getCompensationStatus())
                .build();
    }
}
