package com.bob.db.mapper;

import com.bob.db.dto.QualificationGroupMappingDTO;
import com.bob.db.entity.QualificationGroupMappingEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QualificationGroupMappingMapper {

    QualificationGroupMappingDTO toDTO(QualificationGroupMappingEntity entity);

    QualificationGroupMappingEntity toEntity(QualificationGroupMappingDTO dto);

    List<QualificationGroupMappingDTO> toDTOList(List<QualificationGroupMappingEntity> entities);

    List<QualificationGroupMappingEntity> toEntityList(List<QualificationGroupMappingDTO> dtos);
}