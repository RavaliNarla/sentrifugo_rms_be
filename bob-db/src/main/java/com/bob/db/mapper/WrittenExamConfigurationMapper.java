package com.bob.db.mapper;

import com.bob.db.dto.WrittenExamConfigurationDTO;
import com.bob.db.entity.WrittenExamConfigurationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WrittenExamConfigurationMapper {
    WrittenExamConfigurationMapper INSTANCE = Mappers.getMapper(WrittenExamConfigurationMapper.class);

    @Mapping(target = "sections", ignore = true)
    WrittenExamConfigurationDTO toDTO(WrittenExamConfigurationEntity entity);
    WrittenExamConfigurationEntity toEntity(WrittenExamConfigurationDTO dto);
    @Mapping(target = "positionId", ignore = true)
    void updateEntity(WrittenExamConfigurationDTO dto, @MappingTarget WrittenExamConfigurationEntity entity);
    List<WrittenExamConfigurationDTO> toDTOs(List<WrittenExamConfigurationEntity> entities);
    List<WrittenExamConfigurationEntity> toEntities(List<WrittenExamConfigurationDTO> dtos);
}
