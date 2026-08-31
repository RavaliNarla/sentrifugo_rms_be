package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingReferenceDTO;
import com.bob.db.entity.PreOnboardingReferenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PreOnboardingReferenceMapper {

    PreOnboardingReferenceEntity toEntity(PreOnboardingReferenceDTO dto);

    PreOnboardingReferenceDTO toDTO(PreOnboardingReferenceEntity entity);

    void updateEntityFromDto(PreOnboardingReferenceDTO dto, @MappingTarget PreOnboardingReferenceEntity entity);

    List<PreOnboardingReferenceEntity> toEntityList(List<PreOnboardingReferenceDTO> dtoList);

    List<PreOnboardingReferenceDTO> toDTOList(List<PreOnboardingReferenceEntity> entityList);
}