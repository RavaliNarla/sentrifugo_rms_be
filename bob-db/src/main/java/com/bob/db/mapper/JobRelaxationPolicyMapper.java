package com.bob.db.mapper;

import com.bob.db.dto.JobRelaxationPolicyDTO;
import com.bob.db.entity.JobRelaxationPolicyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobRelaxationPolicyMapper {
    JobRelaxationPolicyEntity toEntity(JobRelaxationPolicyDTO jobRelaxationPolicyDTO);

    JobRelaxationPolicyDTO toDTO(JobRelaxationPolicyEntity jobRelaxationPolicyEntity);

    List<JobRelaxationPolicyDTO> toDTOList(List<JobRelaxationPolicyEntity> jobRelaxationPolicyEntities);

    List<JobRelaxationPolicyEntity> toEntityList(List<JobRelaxationPolicyDTO> jobRelaxationPolicyDTOS);
}

