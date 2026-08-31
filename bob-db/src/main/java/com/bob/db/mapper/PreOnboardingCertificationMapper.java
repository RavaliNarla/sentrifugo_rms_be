package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingCertificationDTO;
import com.bob.db.entity.PreOnboardingCertificationEntity;
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
public interface PreOnboardingCertificationMapper {

    PreOnboardingCertificationEntity toEntity(PreOnboardingCertificationDTO dto);

    PreOnboardingCertificationDTO toDTO(PreOnboardingCertificationEntity entity);

    void updateEntityFromDto(PreOnboardingCertificationDTO dto, @MappingTarget PreOnboardingCertificationEntity entity);

    List<PreOnboardingCertificationEntity> toEntityList(List<PreOnboardingCertificationDTO> dtoList);

    List<PreOnboardingCertificationDTO> toDTOList(List<PreOnboardingCertificationEntity> entityList);
}