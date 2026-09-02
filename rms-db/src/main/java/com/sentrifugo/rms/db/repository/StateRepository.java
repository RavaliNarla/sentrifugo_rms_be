package com.sentrifugo.rms.db.repository;

import com.sentrifugo.rms.db.entity.StateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StateRepository extends JpaRepository<StateEntity, UUID> {
    List<StateEntity> findAllByOrderByNameAsc();
}
