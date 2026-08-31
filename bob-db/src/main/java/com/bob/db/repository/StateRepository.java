package com.bob.db.repository;

import com.bob.db.entity.StateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface StateRepository extends JpaRepository<StateEntity,UUID> {

    List<StateEntity> findAllByIsActiveTrue();


    List<StateEntity> findAllByIdInAndIsActiveTrue(Collection<UUID> stateIds);

}
