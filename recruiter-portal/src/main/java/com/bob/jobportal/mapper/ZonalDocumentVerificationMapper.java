package com.bob.jobportal.mapper;

import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.jobportal.model.ZonalDocumentVerificationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ZonalDocumentVerificationMapper {
    
    CandidateApplicationDocumentVerificationDTO toDTO(ZonalDocumentVerificationRequest request);
}