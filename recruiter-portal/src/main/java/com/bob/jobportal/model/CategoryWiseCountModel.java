package com.bob.jobportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryWiseCountModel {
    private UUID categoryId;
    private Integer appeared;
    private Integer qualifiedWithoutRelaxation;
    private Integer vacancy;


    public void incrementAppeared() {
        appeared++;
    }

    public void incrementQualified() {
        qualifiedWithoutRelaxation++;
    }

    public void incrementVacancy(int count) {
        vacancy += count;
    }
}
