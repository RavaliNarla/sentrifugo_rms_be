package com.bob.db.repository;

import com.bob.db.entity.ExclusionsMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExclusionsMasterRepository extends JpaRepository<ExclusionsMasterEntity, UUID> {

}

