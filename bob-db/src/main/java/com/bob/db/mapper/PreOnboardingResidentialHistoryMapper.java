package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingResidentialHistoryDTO;
import com.bob.db.entity.PreOnboardingResidentialHistoryEntity;
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
public interface PreOnboardingResidentialHistoryMapper {

    PreOnboardingResidentialHistoryEntity toEntity(PreOnboardingResidentialHistoryDTO dto);

    PreOnboardingResidentialHistoryDTO toDTO(PreOnboardingResidentialHistoryEntity entity);

    void updateEntityFromDto(PreOnboardingResidentialHistoryDTO dto, @MappingTarget PreOnboardingResidentialHistoryEntity entity);

    List<PreOnboardingResidentialHistoryEntity> toEntityList(List<PreOnboardingResidentialHistoryDTO> dtoList);

    List<PreOnboardingResidentialHistoryDTO> toDTOList(List<PreOnboardingResidentialHistoryEntity> entityList);
}