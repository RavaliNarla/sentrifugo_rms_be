package com.bob.db.mapper;

import com.bob.db.dto.ApplicationFeeDTO;
import com.bob.db.dto.ApplicationFeeDTO;
import com.bob.db.entity.ApplicationFeeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationFeeMapper {
    ApplicationFeeDTO toDTO(ApplicationFeeEntity entity);
    ApplicationFeeEntity toEntity(ApplicationFeeDTO dto);
    List<ApplicationFeeDTO> toDTOList(List<ApplicationFeeEntity> entities);
    List<ApplicationFeeEntity> toEntityList(List<ApplicationFeeDTO> dtos);
    void updateEntityFromDto(ApplicationFeeDTO dto, @MappingTarget ApplicationFeeEntity entity);

}
