package com.bob.db.repository;

import com.bob.db.dto.InterviewCentresDTO;
import com.bob.db.entity.InterviewCentresEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewCentresRepository extends JpaRepository<InterviewCentresEntity, UUID> , JpaSpecificationExecutor<InterviewCentresEntity> {

    List<InterviewCentresEntity> findAllByIdIn(List<UUID> ids);

    List<InterviewCentresEntity> findAllByZonalStateIdIn(Iterable<UUID> ids);

}

