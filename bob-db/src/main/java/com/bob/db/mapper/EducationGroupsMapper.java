package com.bob.db.mapper;

import com.bob.db.dto.EducationGroupsDTO;
import com.bob.db.entity.EducationGroupsEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EducationGroupsMapper {

    EducationGroupsDTO toDTO(EducationGroupsEntity entity);

    EducationGroupsEntity toEntity(EducationGroupsDTO dto);

    List<EducationGroupsDTO> toDTOList(List<EducationGroupsEntity> entities);

    List<EducationGroupsEntity> toEntityList(List<EducationGroupsDTO> dtos);
}