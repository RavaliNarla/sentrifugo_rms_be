package com.bob.jobportal.util;

import com.bob.jobportal.model.CategoryWiseCountModel;
import lombok.Data;

import java.util.*;

@Data
public class SummaryTracker {
    private final UUID stateId;

    private final Map<UUID, CategoryWiseCountModel> stats =
            new HashMap<>();

    public SummaryTracker() {
        this.stateId = null;
    }

    public SummaryTracker(UUID stateId) {
        this.stateId = stateId;
    }

    private CategoryWiseCountModel getStats(UUID categoryId) {

        return stats.computeIfAbsent(
                categoryId,
                k -> CategoryWiseCountModel.builder()
                        .categoryId(categoryId)
                        .appeared(0)
                        .qualifiedWithoutRelaxation(0)
                        .vacancy(0)
                        .build()
        );
    }

    public void addAppeared(UUID categoryId) {
        getStats(categoryId).incrementAppeared();
    }

    public void addQualified(UUID categoryId) {
        getStats(categoryId).incrementQualified();
    }

    public void addVacancy(UUID categoryId, int count) {
        getStats(categoryId).incrementVacancy(count);
    }

    public int getVacancyCount(UUID categoryId) {

        CategoryWiseCountModel stat = stats.get(categoryId);

        return stat != null ? stat.getVacancy() : 0;
    }

    public int getTotalAppeared() {

        return stats.values()
                .stream()
                .mapToInt(CategoryWiseCountModel::getAppeared)
                .sum();
    }

    public int getTotalQualified() {

        return stats.values()
                .stream()
                .mapToInt(CategoryWiseCountModel::getQualifiedWithoutRelaxation)
                .sum();
    }

    public int getTotalVacancy(UUID categoryId) {

        return stats.values()
                .stream()
                .filter((curr)->curr.getCategoryId() != categoryId)
                .mapToInt(CategoryWiseCountModel::getVacancy)
                .sum();
    }

    public List<CategoryWiseCountModel> getCategorySummaries() {
        return new ArrayList<>(stats.values());
    }
}
