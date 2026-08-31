package com.bob.db.mapper;

import com.bob.db.dto.ExclusionsMasterDTO;
import com.bob.db.entity.ExclusionsMasterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExclusionsMasterMapper {
    ExclusionsMasterMapper INSTANCE = Mappers.getMapper(ExclusionsMasterMapper.class);

    ExclusionsMasterDTO toDto(ExclusionsMasterEntity entity);

    ExclusionsMasterEntity toEntity(ExclusionsMasterDTO dto);

    List<ExclusionsMasterDTO> toDtoList(List<ExclusionsMasterEntity> entities);

    List<ExclusionsMasterEntity> toEntityList(List<ExclusionsMasterDTO> dtos);
}

