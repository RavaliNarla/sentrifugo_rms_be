package com.bob.masterdata.Service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.db.dto.SkillDTO;
import com.bob.db.entity.SkillEntity;
import com.bob.db.mapper.SkillMapper;
import com.bob.db.repository.SkillRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SkillService {
    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    public SkillDTO createSkill(SkillDTO skillDto) {
        try {
            SkillEntity skill=skillMapper.toEntity(skillDto);
            skillRepository.save(skill);
            return skillMapper.toDto(skill);
        } catch (Exception e) {
            return null;
        }
    }

    public List<SkillDTO> getAllSkills(){
        try {
            return skillMapper.toDtoList(skillRepository.findAll());
        } catch (Exception e) {
            throw new CommonException("Failed to fetch Skills");
        }
    }

    public SkillDTO updateSKill(UUID id, SkillDTO skillDto) {
        SkillEntity skillEntity=skillRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Skill not found"));
        skillMapper.updateEntityFromDto(skillDto,skillEntity);
        skillRepository.save(skillEntity);
        return skillMapper.toDto(skillEntity);
    }

    public SkillDTO deleteSkill(UUID id) {

        SkillEntity skillEntity =skillRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Skill not found"));
        skillRepository.delete(skillEntity);
        return skillMapper.toDto(skillEntity);

    }

}
