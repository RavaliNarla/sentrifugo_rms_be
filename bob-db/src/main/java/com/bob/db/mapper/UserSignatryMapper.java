package com.bob.db.mapper;


import com.bob.db.dto.UserSignatryDto;

import com.bob.db.entity.UserSignatryEntity;
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
public interface UserSignatryMapper {
    UserSignatryDto toDTO(UserSignatryEntity entity);
    List<UserSignatryDto> toDTOList(List<UserSignatryEntity> entities);
    UserSignatryEntity toEntity(UserSignatryDto dto);
    void updateEntityFromDto(UserSignatryDto dto,@MappingTarget UserSignatryEntity entity);
}
