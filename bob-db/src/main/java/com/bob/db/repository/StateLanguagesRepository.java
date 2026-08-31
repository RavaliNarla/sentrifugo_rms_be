package com.bob.db.repository;

import com.bob.db.entity.StateLanguagesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface StateLanguagesRepository extends JpaRepository<StateLanguagesEntity, UUID> {
    @Modifying
    @Query("DELETE FROM StateLanguagesEntity s WHERE s.stateId = :stateId")
    void deleteByStateId(UUID stateId);
}
