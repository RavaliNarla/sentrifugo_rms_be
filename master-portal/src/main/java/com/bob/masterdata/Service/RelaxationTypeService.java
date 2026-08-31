package com.bob.masterdata.Service;


import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.dto.RelaxationTypesDTO;
import com.bob.db.entity.RelaxationTypesEntity;
import com.bob.db.mapper.RelaxationTypesMapper;
import com.bob.db.repository.RelaxationTypesRepository;
import com.bob.commonutil.util.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RelaxationTypeService {

    @Autowired
    private RelaxationTypesRepository relaxationTypesRepository;

    @Autowired
    private RelaxationTypesMapper relaxationTypesMapper;
    public RelaxationTypesDTO addRelaxationType(RelaxationTypesDTO dto) {
        RelaxationTypesEntity relaxationTypeEntity = relaxationTypesMapper.toEntity(dto);
        return relaxationTypesMapper.toDTO(relaxationTypesRepository.save(relaxationTypeEntity));
    }

    public RelaxationTypesDTO updateRelaxationType(UUID id, RelaxationTypesDTO dto) {
        RelaxationTypesEntity existingEntity = relaxationTypesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Relaxation Type not found."));
        relaxationTypesMapper.updateEntityFromDto(dto,existingEntity);
        return relaxationTypesMapper.toDTO(relaxationTypesRepository.save(existingEntity));
    }

    public List<RelaxationTypesDTO> getAllRelaxationTypes() {
        List<RelaxationTypesEntity> entities = relaxationTypesRepository.findAll(Sort.by(Sort.Direction.DESC,
                AppConstants.MASTER_CREATED_DATE));
        return relaxationTypesMapper.toDTOList(entities);
    }

    public void deleteRelaxationType(UUID id) {
        relaxationTypesRepository.deleteById(id);
    }
}
