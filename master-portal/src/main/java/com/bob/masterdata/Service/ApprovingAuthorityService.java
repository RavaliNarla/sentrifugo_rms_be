package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.dto.ApprovingAuthorityDTO;
import com.bob.db.entity.ApprovingAuthorityEntity;
import com.bob.db.mapper.ApprovingAuthorityMapper;
import com.bob.db.repository.ApprovingAuthorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ApprovingAuthorityService {

    @Autowired
    private ApprovingAuthorityRepository approvingAuthorityRepository;

    @Autowired
    private ApprovingAuthorityMapper approvingAuthorityMapper;

    public ApprovingAuthorityDTO create(ApprovingAuthorityDTO dto) {
        ApprovingAuthorityEntity saved = approvingAuthorityRepository.save(approvingAuthorityMapper.toEntity(dto));
        return approvingAuthorityMapper.toDto(saved);
    }

    public List<ApprovingAuthorityDTO> getAll(){
        try {
            return approvingAuthorityMapper.toDtoList(approvingAuthorityRepository.findAll());
        } catch (Exception e) {
            throw new CommonException("Failed to fetch approving authorities");
        }
    }

    public ApprovingAuthorityDTO update(UUID id, ApprovingAuthorityDTO dto) {
        ApprovingAuthorityEntity entity = approvingAuthorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approving authority not found"));
        approvingAuthorityMapper.updateEntityFromDto(dto, entity);
        return approvingAuthorityMapper.toDto(approvingAuthorityRepository.save(entity));
    }

    public ApprovingAuthorityDTO delete(UUID id) {
        ApprovingAuthorityEntity entity = approvingAuthorityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Approving authority not found"));
        approvingAuthorityRepository.delete(entity);
        return approvingAuthorityMapper.toDto(entity);
    }

}
