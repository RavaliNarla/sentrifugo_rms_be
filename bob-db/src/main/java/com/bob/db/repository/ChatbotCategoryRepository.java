package com.bob.db.repository;

import com.bob.db.entity.ChatbotCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatbotCategoryRepository extends JpaRepository<ChatbotCategoryEntity, UUID> {

    @Query("SELECT DISTINCT c.parentCategory FROM ChatbotCategoryEntity c")
    List<String> findDistinctParentCategories();

    List<ChatbotCategoryEntity> findByParentCategory(String parentCategory);

    List<ChatbotCategoryEntity> findByCategoryName(String categoryName);

    List<ChatbotCategoryEntity> findByQuestion(String question);
}


