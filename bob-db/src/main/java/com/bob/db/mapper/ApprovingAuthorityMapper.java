package com.bob.db.mapper;

import com.bob.db.dto.ApprovingAuthorityDTO;
import com.bob.db.entity.ApprovingAuthorityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ApprovingAuthorityMapper {

    ApprovingAuthorityDTO toDto(ApprovingAuthorityEntity entity);
    List<ApprovingAuthorityDTO> toDtoList(List<ApprovingAuthorityEntity> entities);
    ApprovingAuthorityEntity toEntity(ApprovingAuthorityDTO dto);
    void updateEntityFromDto(ApprovingAuthorityDTO dto, @MappingTarget ApprovingAuthorityEntity entity);

}
