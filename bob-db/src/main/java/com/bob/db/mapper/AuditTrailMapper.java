package com.bob.db.mapper;

import com.bob.db.dto.AuditTrailDTO;
import com.bob.db.entity.AuditTrailEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditTrailMapper {

    AuditTrailDTO toDto(AuditTrailEntity entity);

    AuditTrailEntity toEntity(AuditTrailDTO dto);

    List<AuditTrailDTO> toDtoList(List<AuditTrailEntity> entities);

    List<AuditTrailEntity> toEntityList(List<AuditTrailDTO> dtos);
}
