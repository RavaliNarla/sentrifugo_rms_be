package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingFamilyDetailsDTO;
import com.bob.db.entity.PreOnboardingFamilyDetailsEntity;
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
public interface PreOnboardingFamilyDetailsMapper {

    PreOnboardingFamilyDetailsEntity toEntity(PreOnboardingFamilyDetailsDTO dto);

    PreOnboardingFamilyDetailsDTO toDTO(PreOnboardingFamilyDetailsEntity entity);

    void updateEntityFromDto(PreOnboardingFamilyDetailsDTO dto, @MappingTarget PreOnboardingFamilyDetailsEntity entity);

    List<PreOnboardingFamilyDetailsEntity> toEntityList(List<PreOnboardingFamilyDetailsDTO> dtoList);

    List<PreOnboardingFamilyDetailsDTO> toDTOList(List<PreOnboardingFamilyDetailsEntity> entityList);
}