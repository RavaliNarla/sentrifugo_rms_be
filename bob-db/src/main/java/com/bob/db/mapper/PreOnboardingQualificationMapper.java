package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingQualificationDTO;
import com.bob.db.entity.PreOnboardingQualificationEntity;
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
public interface PreOnboardingQualificationMapper {

    PreOnboardingQualificationEntity toEntity(PreOnboardingQualificationDTO dto);

    PreOnboardingQualificationDTO toDTO(PreOnboardingQualificationEntity entity);

    void updateEntityFromDto(PreOnboardingQualificationDTO dto, @MappingTarget PreOnboardingQualificationEntity entity);

    List<PreOnboardingQualificationEntity> toEntityList(List<PreOnboardingQualificationDTO> dtoList);

    List<PreOnboardingQualificationDTO> toDTOList(List<PreOnboardingQualificationEntity> entityList );
}