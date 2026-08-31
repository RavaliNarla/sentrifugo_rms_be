package com.bob.db.mapper;

import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.entity.PreOnboardingDocumentEntity;
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
public interface PreOnboardingDocumentMapper {

    PreOnboardingDocumentEntity toEntity(PreOnboardingDocumentDTO dto);

    PreOnboardingDocumentDTO toDTO(PreOnboardingDocumentEntity entity);

    void updateEntityFromDto(PreOnboardingDocumentDTO dto, @MappingTarget PreOnboardingDocumentEntity entity);

    List<PreOnboardingDocumentEntity> toEntityList(List<PreOnboardingDocumentDTO> dtoList);

    List<PreOnboardingDocumentDTO> toDTOList(List<PreOnboardingDocumentEntity> entityList);
}