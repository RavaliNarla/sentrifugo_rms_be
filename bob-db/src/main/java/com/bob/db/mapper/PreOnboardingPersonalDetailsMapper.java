package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingPersonalDetailsDTO;
import com.bob.db.entity.PreOnboardingPersonalDetailsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {PreOnboardingDocumentMapper.class}
)
public interface PreOnboardingPersonalDetailsMapper {

    PreOnboardingPersonalDetailsEntity toEntity(PreOnboardingPersonalDetailsDTO dto);

    PreOnboardingPersonalDetailsDTO toDTO(PreOnboardingPersonalDetailsEntity entity);

    void updateEntityFromDto(PreOnboardingPersonalDetailsDTO dto, @MappingTarget PreOnboardingPersonalDetailsEntity entity);

    List<PreOnboardingPersonalDetailsEntity> toEntityList(List<PreOnboardingPersonalDetailsDTO> dtoList);

    List<PreOnboardingPersonalDetailsDTO> toDTOList(List<PreOnboardingPersonalDetailsEntity> entityList);
}