package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DashboardDateRangePreset {


        FINANCIAL_YEAR,
        CALENDAR_YEAR,
        QUARTER,
        CUSTOM;

        @JsonCreator
        public static DashboardDateRangePreset fromValue(String value) {
            if (value == null) {
                return null ;
            }

            for (DashboardDateRangePreset type : DashboardDateRangePreset.values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Invalid DateRangeType: " + value);
        }
}

