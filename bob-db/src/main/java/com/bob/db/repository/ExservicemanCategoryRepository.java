package com.bob.db.repository;


import java.util.UUID;

import com.bob.db.entity.ExservicemanCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExservicemanCategoryRepository extends JpaRepository<ExservicemanCategoryEntity, UUID> {
}
