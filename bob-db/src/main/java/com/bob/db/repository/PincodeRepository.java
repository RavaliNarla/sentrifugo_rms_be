// ...existing code...
package com.bob.db.repository;

import com.bob.db.entity.PincodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PincodeRepository extends JpaRepository<PincodeEntity, UUID> {

}

