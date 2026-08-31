package com.bob.db.mapper;

import com.bob.db.dto.TemplatesDTO;
import com.bob.db.entity.TemplatesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TemplatesMapper {

    TemplatesEntity toEntity(TemplatesDTO templatesDTO);

    TemplatesDTO toDto(TemplatesEntity templatesEntity);

    List<TemplatesEntity> toEntityList(List<TemplatesDTO> templatesDTOs);

    List<TemplatesDTO> toDtoList(List<TemplatesEntity> templatesEntities);

    void updateEntityFromDto(TemplatesDTO templatesDTO, @MappingTarget TemplatesEntity templatesEntity);
}
