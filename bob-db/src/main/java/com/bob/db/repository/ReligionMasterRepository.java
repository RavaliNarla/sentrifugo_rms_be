package com.bob.db.repository;

import com.bob.db.entity.ReligionMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReligionMasterRepository extends JpaRepository<ReligionMasterEntity, UUID> {
}
