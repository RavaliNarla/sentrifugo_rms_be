package com.bob.db.mapper;

import com.bob.db.dto.DisabilityCategoriesDTO;
import com.bob.db.entity.DisabilityCategoriesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DisabilityCategoriesMapper {

    DisabilityCategoriesDTO toDto(DisabilityCategoriesEntity disabilityCategoriesEntity);
    DisabilityCategoriesEntity toEntity(DisabilityCategoriesDTO disabilityCategoriesDTO);
    List<DisabilityCategoriesDTO> toDtoList(List<DisabilityCategoriesEntity> disabilityCategoriesEntities);
    List<DisabilityCategoriesEntity> toEntityList(List<DisabilityCategoriesDTO> disabilityCategoriesDTOS);
    void updateEntityFromDto(DisabilityCategoriesDTO disabilityCategoriesDTO,@MappingTarget DisabilityCategoriesEntity disabilityCategoriesEntity);
}
