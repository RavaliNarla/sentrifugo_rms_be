package com.bob.db.mapper;

import com.bob.db.dto.InterviewCommitteeDTO;
import com.bob.db.entity.InterviewCommitteeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InterviewCommitteeMapper {
    InterviewCommitteeDTO toDTO(InterviewCommitteeEntity entity);
    InterviewCommitteeEntity toEntity(InterviewCommitteeDTO dto);
    List<InterviewCommitteeEntity> toEntityList(List<InterviewCommitteeDTO> dtoList);
    List<InterviewCommitteeDTO> toDTOList(List<InterviewCommitteeEntity> entityList);
}
