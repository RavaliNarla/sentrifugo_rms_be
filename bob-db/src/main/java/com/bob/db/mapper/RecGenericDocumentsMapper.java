package com.bob.db.mapper;

import com.bob.db.dto.RecGenericDocumentsDTO;
import com.bob.db.entity.RecGenericDocumentsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RecGenericDocumentsMapper {

    RecGenericDocumentsDTO toDto(RecGenericDocumentsEntity entity);
    List<RecGenericDocumentsDTO> toDtoList(List<RecGenericDocumentsEntity> entities);
    RecGenericDocumentsEntity toEntity(RecGenericDocumentsDTO dto);
    List<RecGenericDocumentsEntity> toEntityList(List<RecGenericDocumentsDTO> dtos);
    void updateEntityFromDto(RecGenericDocumentsDTO dto, @MappingTarget RecGenericDocumentsEntity entity);

}

