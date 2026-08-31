package com.bob.masterdata.Service;


import com.bob.db.dto.LanguageMasterDTO;
import com.bob.db.dto.StateLanguagesDTO;
import com.bob.db.entity.StateLanguagesEntity;
import com.bob.db.mapper.LanguageMasterMapper;
import com.bob.db.mapper.StateLanguagesMapper;
import com.bob.db.repository.LanguageMasterRepository;
import com.bob.db.repository.StateLanguagesRepository;
import com.bob.masterdata.Model.StateLanguageModel;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LanguageMasterService {
    @Autowired
    private StateLanguagesRepository stateLanguagesRepository;

    @Autowired
    private StateLanguagesMapper stateLanguagesMapper;

    public List<StateLanguagesDTO> getAllLanguagesByStateIds(List<UUID> stateIds) {
        return stateLanguagesMapper.toDtoList(stateLanguagesRepository.findAllById(stateIds));
    }

    public List<StateLanguagesDTO> getAllLanguages() {
        return stateLanguagesMapper.toDtoList(stateLanguagesRepository.findAll());
    }

    @Transactional
    public List<StateLanguagesDTO> createOrUpdateStateLanguage(StateLanguageModel stateLanguageModel) {
        UUID stateId = stateLanguageModel.getStateId();
        List<UUID> languageIds = stateLanguageModel.getLanguageIds();
        //Delete existing entries for the state
        stateLanguagesRepository.deleteByStateId(stateId);
        //Create new entries
        List<StateLanguagesEntity> entities=new ArrayList<>();
        for(UUID languageId:languageIds){
            StateLanguagesEntity stateLanguagesEntity= StateLanguagesEntity.builder()
                    .stateId(stateId)
                    .languageId(languageId)
                    .build();
            entities.add(stateLanguagesEntity);
        }
        List<StateLanguagesEntity> savedEntities = stateLanguagesRepository.saveAll(entities);
        // Fetch the state name for mapping
        return stateLanguagesMapper.toDtoList(savedEntities);
    }

}
