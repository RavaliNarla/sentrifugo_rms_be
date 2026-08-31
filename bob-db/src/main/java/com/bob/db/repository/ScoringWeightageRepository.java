package com.bob.db.repository;


import com.bob.db.entity.ScoringWeightageEntity;
import com.bob.db.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScoringWeightageRepository extends JpaRepository<ScoringWeightageEntity, UUID> {

}