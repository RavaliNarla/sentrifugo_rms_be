package com.bob.db.mapper;

import com.bob.db.dto.PincodeDTO;
import com.bob.db.entity.PincodeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PincodeMapper {
    PincodeDTO toDTO(PincodeEntity entity);

    PincodeEntity toEntity(PincodeDTO dto);

    List<PincodeDTO> toDTOList(List<PincodeEntity> entities);

    List<PincodeEntity> toEntityList(List<PincodeDTO> dtos);

    void updateEntityFromDto(PincodeDTO dto,
                             @MappingTarget PincodeEntity entity);
}

