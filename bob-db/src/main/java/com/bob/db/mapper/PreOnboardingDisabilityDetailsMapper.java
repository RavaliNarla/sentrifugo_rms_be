package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingDisabilityDetailsDTO;
import com.bob.db.entity.PreOnboardingDisabilityDetailsEntity;
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
public interface PreOnboardingDisabilityDetailsMapper {

    PreOnboardingDisabilityDetailsEntity toEntity(PreOnboardingDisabilityDetailsDTO dto);

    PreOnboardingDisabilityDetailsDTO toDTO(PreOnboardingDisabilityDetailsEntity entity);

    void updateEntityFromDto(PreOnboardingDisabilityDetailsDTO dto, @MappingTarget PreOnboardingDisabilityDetailsEntity entity);

    List<PreOnboardingDisabilityDetailsEntity> toEntityList(List<PreOnboardingDisabilityDetailsDTO> dtoList);

    List<PreOnboardingDisabilityDetailsDTO> toDTOList(List<PreOnboardingDisabilityDetailsEntity> entityList);
}