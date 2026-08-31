package com.bob.jobportal.util;




import com.bob.db.entity.DisabilityCategoriesEntity;
import com.bob.db.repository.DisabilityCategoriesRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Getter
public class PwdCategoryCache {

    @Autowired
    private DisabilityCategoriesRepository repository;

    private UUID hiCategoryId;
    private UUID viCategoryId;
    private UUID ocCategoryId;
    private UUID idCategoryId;

    /*
     * CATEGORY CODE MAP
     */
    private final Map<UUID, String> categoryCodeMap = new HashMap<>();

    @PostConstruct
    public void init() {

        List<DisabilityCategoriesEntity> categories = repository.findAll();

        Map<String, UUID> map =
                categories.stream()
                        .collect(Collectors.toMap(
                                c -> c.getDisabilityCode().toUpperCase(),
                                DisabilityCategoriesEntity::getId
                        ));

        hiCategoryId = map.get("HI");
        viCategoryId = map.get("VI");
        ocCategoryId = map.get("OC");
        idCategoryId = map.get("ID");

        /*
         * CATEGORY CODE MAP
         */
        for (DisabilityCategoriesEntity category : categories) {

            String code = category.getDisabilityCode().toUpperCase();

            categoryCodeMap.put(
                    category.getId(),
                    code
            );
        }
    }

    public String getCategoryCode(UUID categoryId) {
        return categoryCodeMap.get(categoryId);
    }
}