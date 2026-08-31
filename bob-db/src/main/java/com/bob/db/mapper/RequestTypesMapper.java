package com.bob.db.mapper;

import com.bob.db.dto.RequestTypesDTO;
import com.bob.db.entity.RequestTypesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RequestTypesMapper {

    RequestTypesEntity toEntity(RequestTypesDTO requestTypesDTO);

    RequestTypesDTO toDTO(RequestTypesEntity requestTypesEntity);

    List<RequestTypesDTO> toDTOList(List<RequestTypesEntity> requestTypesEntityList);

    List<RequestTypesEntity> toEntityList(List<RequestTypesDTO> requestTypesDTOList);

    void updateEntityFromDto(RequestTypesDTO requestTypesDTO,
                             @MappingTarget RequestTypesEntity requestTypesEntity);
}

