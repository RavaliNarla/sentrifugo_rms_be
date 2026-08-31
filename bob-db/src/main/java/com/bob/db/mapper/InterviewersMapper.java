package com.bob.db.mapper;

import com.bob.db.dto.InterviewersDTO;
import com.bob.db.entity.InterviewersEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InterviewersMapper {
    InterviewersDTO toDto(InterviewersEntity interviewersEntity);
    InterviewersEntity toEntity(InterviewersDTO interviewersDTO);
    List<InterviewersDTO> toDtoList(List<InterviewersEntity> interviewersEntities);

    List<InterviewersEntity> toEntityList(List<InterviewersDTO> interviewersDTOS);
    void updateEntityFromDto(InterviewersDTO interviewersDTO,@MappingTarget InterviewersEntity interviewersEntity);
}
