package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.ApprovedByRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovedByRoleRepository extends JpaRepository<ApprovedByRoleEntity, UUID> {
    List<ApprovedByRoleEntity> findAllByOrderByNameAsc();
}
