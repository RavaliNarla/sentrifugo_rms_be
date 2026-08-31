package com.bob.masterdata.Service;


import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.StateDTO;
import com.bob.db.entity.StateEntity;
import com.bob.db.mapper.StateMapper;
import com.bob.db.repository.StateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StateService {
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private StateMapper stateMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public StateDTO createState(StateDTO stateDto) {
        try {
            return stateMapper.toDto(stateRepository.save(stateMapper.toEntity(stateDto)));
        } catch (Exception e) {
            return null;
        }
    }

    public List<StateDTO> getAllStates(){
        try {
            return stateMapper.toDtoList(stateRepository.findAll());
        } catch (Exception e) {
            throw new CommonException("Failed to fetch states");
        }
    }

    public StateDTO updateState(UUID id, StateDTO state) {
        StateEntity stateEntity=stateRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("State not found"));
        stateMapper.updateEntityFromDto(state,stateEntity);
        StateEntity updated=stateRepository.save(stateEntity);
        return stateMapper.toDto(updated);
    }

    public StateDTO deleteState(UUID id) {

                StateEntity state=stateRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("State not found"));
                stateRepository.delete(state);
                return stateMapper.toDto(state);

    }
}
