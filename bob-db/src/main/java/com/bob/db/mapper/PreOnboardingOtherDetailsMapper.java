package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingOtherDetailsDTO;
import com.bob.db.entity.PreOnboardingOtherDetailsEntity;
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
public interface PreOnboardingOtherDetailsMapper {

    PreOnboardingOtherDetailsEntity toEntity(PreOnboardingOtherDetailsDTO dto);

    PreOnboardingOtherDetailsDTO toDTO(PreOnboardingOtherDetailsEntity entity);

    void updateEntityFromDto(PreOnboardingOtherDetailsDTO dto, @MappingTarget PreOnboardingOtherDetailsEntity entity);

    List<PreOnboardingOtherDetailsDTO> toDtoList(List<PreOnboardingOtherDetailsEntity> entities);

    List<PreOnboardingOtherDetailsEntity> toEntityList(List<PreOnboardingOtherDetailsDTO> dtoList);
}