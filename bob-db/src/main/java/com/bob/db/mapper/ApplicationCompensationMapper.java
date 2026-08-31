package com.bob.db.mapper;

import com.bob.db.dto.ApplicationCompensationDTO;
import com.bob.db.entity.ApplicationCompensationEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationCompensationMapper {
    ApplicationCompensationDTO toDTO(ApplicationCompensationEntity entity);
    ApplicationCompensationEntity toEntity(ApplicationCompensationDTO dto);
    List<ApplicationCompensationDTO> toDTOList(List<ApplicationCompensationEntity> entities);
    List<ApplicationCompensationEntity> toEntityList(List<ApplicationCompensationDTO> dtos);
    void updateEntityFromDto(ApplicationCompensationDTO dto, @MappingTarget ApplicationCompensationEntity entity);
}

