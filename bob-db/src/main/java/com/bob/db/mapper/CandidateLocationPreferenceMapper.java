package com.bob.db.mapper;

import com.bob.db.dto.CandidateLocationPreferenceDTO;
import com.bob.db.entity.CandidateLocationPreferenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateLocationPreferenceMapper {
    CandidateLocationPreferenceEntity toEntity(CandidateLocationPreferenceDTO candidateLocationPreferenceDTO);

    CandidateLocationPreferenceDTO toDTO(CandidateLocationPreferenceEntity candidateLocationPreferenceEntity);

    List<CandidateLocationPreferenceDTO> toDTOList(List<CandidateLocationPreferenceEntity> candidateLocationPreferenceEntities);

    List<CandidateLocationPreferenceEntity> toEntityList(List<CandidateLocationPreferenceDTO> candidateLocationPreferenceDTOS);

    void updateEntityFromDto(CandidateLocationPreferenceDTO candidateLocationPreferenceDTO, @MappingTarget CandidateLocationPreferenceEntity candidateLocationPreferenceEntity);

}
