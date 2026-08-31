package com.bob.db.mapper;

import com.bob.db.dto.CandidateDisabilityDetailsDTO;
import com.bob.db.entity.CandidateDisabilityDetailsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateDisabilityDetailsMapper {

    CandidateDisabilityDetailsEntity toEntity(CandidateDisabilityDetailsDTO candidateDisabilityDetailsDTO);

    CandidateDisabilityDetailsDTO toDTO(CandidateDisabilityDetailsEntity candidateDisabilityDetailsEntity);

    List<CandidateDisabilityDetailsDTO> toDTOList(List<CandidateDisabilityDetailsEntity> candidateDisabilityEntities);

    List<CandidateDisabilityDetailsEntity> toEntityList(List<CandidateDisabilityDetailsDTO> candidateDisabilityDetailsDTOS);

    void updateEntityFromDto(CandidateDisabilityDetailsDTO candidateDisabilityDetailsDTO, @MappingTarget CandidateDisabilityDetailsEntity candidateDisabilityDetailsEntity);

}

