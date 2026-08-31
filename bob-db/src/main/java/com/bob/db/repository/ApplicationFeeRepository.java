package com.bob.db.repository;

import com.bob.db.entity.ApplicationFeeEntity;
import com.bob.db.enums.ApplicationFeeCategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationFeeRepository extends JpaRepository<ApplicationFeeEntity, UUID> {
    ApplicationFeeEntity findByCategoryCode(ApplicationFeeCategoryCode categoryCode);
}
