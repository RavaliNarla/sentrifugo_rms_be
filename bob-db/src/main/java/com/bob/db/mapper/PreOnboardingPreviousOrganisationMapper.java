package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingPreviousOrganisationDTO;
import com.bob.db.entity.PreOnboardingPreviousOrganisationEntity;
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
public interface PreOnboardingPreviousOrganisationMapper {

    PreOnboardingPreviousOrganisationEntity toEntity(PreOnboardingPreviousOrganisationDTO dto);

    PreOnboardingPreviousOrganisationDTO toDTO(PreOnboardingPreviousOrganisationEntity entity);

    void updateEntityFromDto(PreOnboardingPreviousOrganisationDTO dto, @MappingTarget PreOnboardingPreviousOrganisationEntity entity);

    List<PreOnboardingPreviousOrganisationEntity> toEntityList(List<PreOnboardingPreviousOrganisationDTO> dtoList);

    List<PreOnboardingPreviousOrganisationDTO> toDTOList(List<PreOnboardingPreviousOrganisationEntity> entityList);
}