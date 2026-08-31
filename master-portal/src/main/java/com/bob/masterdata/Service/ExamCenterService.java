package com.bob.masterdata.Service;

import com.bob.db.dto.ExamCenterDTO;
import com.bob.db.entity.ExamCenterEntity;
import com.bob.db.mapper.ExamCenterMapper;
import com.bob.db.repository.ExamCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamCenterService {
    @Autowired
    private ExamCenterRepository examCenterRepository;

    @Autowired
    private ExamCenterMapper examCenterMapper;

    public List<ExamCenterDTO> getAllExamCenters() {
       return examCenterMapper.toDtoList(examCenterRepository.findAll());
    }

}
