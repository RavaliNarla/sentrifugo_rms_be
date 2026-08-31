package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingDTO;
import com.bob.db.entity.PreOnboardingEntity;
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
public interface PreOnboardingMapper {

    PreOnboardingEntity toEntity(PreOnboardingDTO dto);

    PreOnboardingDTO toDTO(PreOnboardingEntity entity);

    void updateEntityFromDto(
            PreOnboardingDTO dto,
            @MappingTarget PreOnboardingEntity entity
    );

    List<PreOnboardingEntity> toEntityList(List<PreOnboardingDTO> dtoList);

    List<PreOnboardingDTO> toDTOList(List<PreOnboardingEntity> entityList);
}