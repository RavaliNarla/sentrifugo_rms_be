package com.bob.db.repository;

import com.bob.db.entity.InterviewCommitteeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewCommitteeRepository extends JpaRepository<InterviewCommitteeEntity, UUID> {
    InterviewCommitteeEntity findByCommitteeName(String interviewCommiteeName);
}
