package com.bob.db.repository;

import com.bob.db.entity.InterviewsEntity;

import java.util.List;

public interface InterviewsRepositoryCustom  {
    InterviewsEntity saveWithAudit(InterviewsEntity oldEntity, InterviewsEntity newEntity);
    List<InterviewsEntity> saveAllWithAudit(List<InterviewsEntity> oldEntities, List<InterviewsEntity> newEntities);
}

