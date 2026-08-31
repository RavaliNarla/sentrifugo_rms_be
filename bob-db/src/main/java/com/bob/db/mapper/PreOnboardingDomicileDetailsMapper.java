package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingDomicileDetailsDTO;
import com.bob.db.entity.PreOnboardingDomicileDetailsEntity;
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
public interface PreOnboardingDomicileDetailsMapper {

    PreOnboardingDomicileDetailsEntity toEntity(PreOnboardingDomicileDetailsDTO dto);

    PreOnboardingDomicileDetailsDTO toDTO(PreOnboardingDomicileDetailsEntity entity);

    void updateEntityFromDto(PreOnboardingDomicileDetailsDTO dto, @MappingTarget PreOnboardingDomicileDetailsEntity entity);

    List<PreOnboardingDomicileDetailsEntity> toEntityList(List<PreOnboardingDomicileDetailsDTO> dtoList);

    List<PreOnboardingDomicileDetailsDTO> toDTOList(List<PreOnboardingDomicileDetailsEntity> entityList);
}