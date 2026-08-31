package com.bob.db.mapper;

import com.bob.db.dto.UserDTO;
import com.bob.db.entity.UserEntity;
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
public interface UserMapper {
    UserDTO toDTO(UserEntity entity);
    List<UserDTO> toDTOList(List<UserEntity> entities);
    UserEntity toEntity(UserDTO dto);
    void updateEntityFromDto(UserDTO dto,@MappingTarget UserEntity entity);
}
