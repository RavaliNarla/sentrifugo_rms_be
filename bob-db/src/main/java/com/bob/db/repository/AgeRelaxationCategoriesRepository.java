package com.bob.db.repository;

import com.bob.db.entity.AgeRelaxationCategoriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgeRelaxationCategoriesRepository extends JpaRepository<AgeRelaxationCategoriesEntity, UUID> {

    Optional<AgeRelaxationCategoriesEntity> findByAgeRelaxationCategoryCode(String ageRelaxationCategoryCode);

    List<AgeRelaxationCategoriesEntity> findByAgeRelaxationCategoryCodeIn(List<String> ageRelaxationCategoryCodes);

}
