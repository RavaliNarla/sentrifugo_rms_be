package com.bob.db.mapper;


import com.bob.db.dto.ExservicemanCategoryDTO;
import com.bob.db.entity.ExservicemanCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ExservicemanCategoryMapper {
    ExservicemanCategoryDTO toDto(ExservicemanCategoryEntity exservicemanCategory);
    ExservicemanCategoryEntity toEntity(ExservicemanCategoryDTO exservicemanCategoryDTO);
    List<ExservicemanCategoryDTO> toDtoList(List<ExservicemanCategoryEntity> exservicemanCategory);
    List<ExservicemanCategoryEntity> toEntityList(List<ExservicemanCategoryDTO> exservicemanCategoryDTO);
}
