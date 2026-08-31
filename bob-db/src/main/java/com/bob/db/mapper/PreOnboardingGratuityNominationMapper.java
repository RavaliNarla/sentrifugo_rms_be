package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingGratuityNominationDTO;
import com.bob.db.entity.PreOnboardingGratuityNominationEntity;
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
public interface PreOnboardingGratuityNominationMapper {

    PreOnboardingGratuityNominationEntity toEntity(PreOnboardingGratuityNominationDTO dto);

    PreOnboardingGratuityNominationDTO toDTO(PreOnboardingGratuityNominationEntity entity);

    void updateEntityFromDto(PreOnboardingGratuityNominationDTO dto, @MappingTarget PreOnboardingGratuityNominationEntity entity);

    List<PreOnboardingGratuityNominationEntity> toEntityList(List<PreOnboardingGratuityNominationDTO> dtoList);

    List<PreOnboardingGratuityNominationDTO> toDTOList(List<PreOnboardingGratuityNominationEntity> entityList);
}