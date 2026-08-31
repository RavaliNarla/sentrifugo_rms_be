package com.bob.db.mapper;

import com.bob.db.dto.StateLanguagesDTO;
import com.bob.db.entity.StateLanguagesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StateLanguagesMapper {

    StateLanguagesDTO toDto(StateLanguagesEntity entity);

    StateLanguagesEntity toEntity(StateLanguagesDTO dto);

    List<StateLanguagesDTO> toDtoList(List<StateLanguagesEntity> entities);

    List<StateLanguagesEntity> toEntityList(List<StateLanguagesDTO> dtos);

    void updateEntityFromDto(StateLanguagesDTO dto, @MappingTarget StateLanguagesEntity entity);

}
