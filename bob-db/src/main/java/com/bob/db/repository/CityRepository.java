package com.bob.db.repository;

import com.bob.db.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<CityEntity,UUID> {


      List<CityEntity> findByStateIdIn(Iterable<UUID> stateIds);
//    List<CityEntity> findAllByCityIdInAndIsActiveTrue(Collection<UUID> cityIds);
}
