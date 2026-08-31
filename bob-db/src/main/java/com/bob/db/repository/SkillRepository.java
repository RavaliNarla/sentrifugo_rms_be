package com.bob.db.repository;

import com.bob.db.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<SkillEntity, UUID> {



//    List<SkillEntity> findAllByIsActiveTrue();
}
