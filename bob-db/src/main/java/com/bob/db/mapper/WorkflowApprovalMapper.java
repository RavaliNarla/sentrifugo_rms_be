package com.bob.db.mapper;

import com.bob.db.dto.WorkflowApprovalDTO;
import com.bob.db.entity.WorkflowApprovalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkflowApprovalMapper {
    WorkflowApprovalMapper INSTANCE = Mappers.getMapper(WorkflowApprovalMapper.class);

    WorkflowApprovalDTO toDTO(WorkflowApprovalEntity entity);
    WorkflowApprovalEntity toEntity(WorkflowApprovalDTO dto);
    List<WorkflowApprovalDTO> toDTOs(List<WorkflowApprovalEntity> entities);
    List<WorkflowApprovalEntity> toEntities(List<WorkflowApprovalDTO> dtos);
}

