package com.bob.jobportal.util;

import com.bob.db.entity.ReservationCategoriesEntity;
import com.bob.db.repository.ReservationCategoriesRepository;
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
public class ReservationCategoryCache {

    @Autowired
    private ReservationCategoriesRepository repository;

    /*
     * CATEGORY IDS
     */
    private UUID generalCategoryId;

    private UUID obcCategoryId;

    private UUID scCategoryId;

    private UUID stCategoryId;

    private UUID ewsCategoryId;

    /*
     * CATEGORY CODE MAP
     */
    private final Map<UUID, String> categoryCodeMap = new HashMap<>();

    @PostConstruct
    public void init() {

        List<ReservationCategoriesEntity> categories = repository.findAll();

        /*
         * NAME -> ID
         */
        Map<String, UUID> map = categories.stream()
                        .collect(Collectors.toMap(
                                entity -> entity.getCategoryCode()
                                        .toUpperCase(),
                                ReservationCategoriesEntity::getId
                        ));

        generalCategoryId = map.get("GEN");

        obcCategoryId = map.get("OBC");

        scCategoryId = map.get("SC");

        stCategoryId = map.get("ST");

        ewsCategoryId = map.get("EWS");

        /*
         * CATEGORY CODE MAP
         */
        for (ReservationCategoriesEntity category : categories) {

            String name = category.getCategoryCode().toUpperCase();

            String code;

            switch (name) {

                case "GEN":
                    code = "UR";
                    break;

                case "OBC":
                    code = "OBC";
                    break;

                case "SC":
                    code = "SC";
                    break;

                case "ST":
                    code = "ST";
                    break;

                case "EWS":
                    code = "EWS";
                    break;

                default:
                    code = name;
            }

            categoryCodeMap.put(
                    category.getId(),
                    code
            );
        }
    }
}