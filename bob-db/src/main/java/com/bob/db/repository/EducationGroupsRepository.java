package com.bob.db.repository;

import com.bob.db.entity.EducationGroupsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EducationGroupsRepository extends JpaRepository<EducationGroupsEntity, UUID> {

}