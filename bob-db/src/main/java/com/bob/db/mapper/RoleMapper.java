package com.bob.db.mapper;

import com.bob.db.dto.RoleDTO;
import com.bob.db.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RoleMapper {

    RoleDTO toDto(RoleEntity roleEntity);

    List<RoleDTO> toDtoList(List<RoleEntity> roleEntities);

    RoleEntity toEntity(RoleDTO roleDTO);

    List<RoleEntity> toEntityList(List<RoleDTO> roleDTOS);

    void updateEntityFromDto(RoleDTO roleDTO, @MappingTarget RoleEntity roleEntity);
}