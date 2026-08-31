package com.bob.db.repository;

import com.bob.db.entity.InterviewersEntity;
import com.bob.db.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface InterviewersRepository extends JpaRepository<InterviewersEntity, UUID> {
    List<InterviewersEntity> findAllByIsActiveTrue();

//    List<InterviewersEntity> findAllByinterviewerIdInAndIsActiveTrue(List<UUID> interviewerIds);


}
