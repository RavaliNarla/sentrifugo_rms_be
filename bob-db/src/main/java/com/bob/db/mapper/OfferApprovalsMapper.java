package com.bob.db.mapper;

import com.bob.db.dto.OfferApprovalsDTO;
import com.bob.db.entity.OfferApprovalsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        // Tell MapStruct to use these external mappers for the nested DTOs
        uses = {
                CandidateApplicationsMapper.class,
                CandidateProfileMapper.class
        }
)
public interface OfferApprovalsMapper {

    // candidateProfile is read-only in the entity, so we ignore it on creation/updates
    @Mapping(target = "candidateProfile", ignore = true)
    OfferApprovalsEntity toEntity(OfferApprovalsDTO dto);

    // MapStruct automatically maps nested DTOs because the field names match perfectly
    OfferApprovalsDTO toDTO(OfferApprovalsEntity entity);

    List<OfferApprovalsDTO> toDTOList(List<OfferApprovalsEntity> entities);

    List<OfferApprovalsEntity> toEntityList(List<OfferApprovalsDTO> dtos);

    @Mapping(target = "candidateProfile", ignore = true)
    void updateEntityFromDto(OfferApprovalsDTO dto, @MappingTarget OfferApprovalsEntity entity);
}