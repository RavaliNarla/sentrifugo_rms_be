package com.bob.db.mapper;

import com.bob.db.dto.StateDTO;
import com.bob.db.entity.StateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StateMapper {

    StateDTO toDto(StateEntity stateEntity);

    List<StateDTO> toDtoList(List<StateEntity> stateEntities);

    StateEntity toEntity(StateDTO stateDTO);
    List<StateEntity> toEntityList(List<StateDTO> stateDTOS);
    void updateEntityFromDto(StateDTO stateDTO,@MappingTarget StateEntity stateEntity);

}
