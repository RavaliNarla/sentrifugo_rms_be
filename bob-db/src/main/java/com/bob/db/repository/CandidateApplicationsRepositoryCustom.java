package com.bob.db.repository;

import com.bob.db.entity.CandidateApplicationsEntity;

import java.util.List;

public interface CandidateApplicationsRepositoryCustom {
    CandidateApplicationsEntity saveWithWorkflow(CandidateApplicationsEntity newEntity);
    public List<CandidateApplicationsEntity> saveAllWithWorkflow(List<CandidateApplicationsEntity> newEntities);
}
