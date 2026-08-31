package com.bob.db.mapper;

import com.bob.db.dto.ScoringWeightageDTO;
import com.bob.db.entity.ScoringWeightageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ScoringWeightageMapper {
    ScoringWeightageDTO toDto(ScoringWeightageEntity scoringWeightageEntity);
    ScoringWeightageEntity toEntity(ScoringWeightageDTO scoringWeightageDTO);
    List<ScoringWeightageDTO> toDtoList(List<ScoringWeightageEntity> scoringWeightageEntities);
    List<ScoringWeightageEntity> toEntityList(List<ScoringWeightageDTO> scoringWeightageDTOS);
    void updateEntityFromDto(ScoringWeightageDTO scoringWeightageDTO, @MappingTarget ScoringWeightageEntity scoringWeightageEntity);
}