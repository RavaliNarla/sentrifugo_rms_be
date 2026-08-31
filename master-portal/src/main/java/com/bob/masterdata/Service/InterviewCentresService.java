package com.bob.masterdata.Service;

import com.bob.db.dto.InterviewCentresDTO;
import com.bob.db.entity.InterviewCentresEntity;
import com.bob.db.mapper.InterviewCentresMapper;
import com.bob.db.repository.InterviewCentresRepository;
import com.bob.masterdata.Model.InterviewCenterRequestModel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewCentresService {

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;




    public List<InterviewCentresDTO> searchInterviewCentresByFilter(InterviewCenterRequestModel model) {
        Specification<InterviewCentresEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(model.getOrganizationTypes() != null && !model.getOrganizationTypes().isEmpty()){
                predicates.add(root.get("organizationType").in(model.getOrganizationTypes()));

            }
           if(model.getZonalStateId() != null){
               predicates.add(criteriaBuilder.equal(root.get("zonalStateId"),model.getZonalStateId()));
           }

           return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<InterviewCentresEntity> interviewCentres = interviewCentresRepository.findAll(spec);

        return interviewCentresMapper.toDtoList(interviewCentres);
    }


}
