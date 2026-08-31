package com.bob.db.mapper;

import com.bob.db.dto.SpecialCategoriesDTO;
import com.bob.db.entity.SpecialCategoriesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SpecialCategoriesMapper {

    SpecialCategoriesDTO toDto(SpecialCategoriesEntity specialCategoriesEntity);

    SpecialCategoriesEntity toEntity(SpecialCategoriesDTO specialCategoriesDTO);

    List<SpecialCategoriesDTO> toDtoList(List<SpecialCategoriesEntity> specialCategoriesEntities);

    List<SpecialCategoriesEntity> toEntityList(List<SpecialCategoriesDTO> specialCategoriesDTOS);
    void updateEntityFromDto(SpecialCategoriesDTO specialCategoriesDTO,@MappingTarget SpecialCategoriesEntity specialCategoriesEntity);

}
