package com.bob.db.repository;

import com.bob.db.entity.JobRelaxationPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRelaxationPolicyRepository extends JpaRepository<JobRelaxationPolicyEntity, UUID> {

    List<JobRelaxationPolicyEntity> findAllByOrderByCreatedDateDesc();
}
