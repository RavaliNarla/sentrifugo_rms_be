package com.bob.commonutil.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VacancyMatrixDTO {

    private UUID positionId;

    private UUID stateId;

    private UUID cityId;

    private List<CategorySeatDTO> categorySeats;

    private List<PwdSeatDTO> pwdSeats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySeatDTO {

        private UUID categoryId;

        private Integer seats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PwdSeatDTO {

        /**
         * PWD reservation category
         */
        private UUID pwdCategoryId;

        /**
         * Optional parent category (SC/ST/OBC/etc)
         * null means horizontal reservation
         */
        private UUID categoryId;

        private Integer seats;
    }
}