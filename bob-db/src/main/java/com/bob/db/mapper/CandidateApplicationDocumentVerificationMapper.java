package com.bob.db.mapper;

import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.db.entity.CandidateApplicationDocumentVerificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateApplicationDocumentVerificationMapper {

    CandidateApplicationDocumentVerificationDTO toDto(CandidateApplicationDocumentVerificationEntity entity);

    List<CandidateApplicationDocumentVerificationDTO> toDtoList(List<CandidateApplicationDocumentVerificationEntity> entities);

    CandidateApplicationDocumentVerificationEntity toEntity(CandidateApplicationDocumentVerificationDTO dto);

    List<CandidateApplicationDocumentVerificationEntity> toEntityList(List<CandidateApplicationDocumentVerificationDTO> dtos);
    void updateEntityFromDto(CandidateApplicationDocumentVerificationDTO dto, @MappingTarget CandidateApplicationDocumentVerificationEntity entity);

}
