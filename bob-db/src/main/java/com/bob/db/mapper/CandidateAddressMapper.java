package com.bob.db.mapper;

import com.bob.db.dto.CandidateAddressDTO;
import com.bob.db.dto.CandidateRegistrationStagesDTO;
import com.bob.db.entity.CandidateAddressEntity;
import com.bob.db.entity.CandidateRegistrationStagesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CandidateAddressMapper {

    CandidateAddressEntity toEntity(CandidateAddressDTO candidateAddressDTO);

    CandidateAddressDTO toDTO(CandidateAddressEntity candidateAddressEntity);

    List<CandidateAddressDTO> toDTOList(List<CandidateAddressEntity> candidateAddressEntities);

    List<CandidateAddressEntity> toEntityList(List<CandidateAddressDTO> candidateAddressDTOS);

    void updateEntityFromDto(CandidateAddressDTO candidateAddressDTO, @MappingTarget CandidateAddressEntity candidateAddressEntity);

}
