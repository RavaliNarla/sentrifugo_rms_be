package com.bob.db.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
public enum DashboardScreen {

    TOTAL_VACANCIES("fn_detail_total_vacancies",
            List.of("Requisition ID","Department","Position","Vacancies")),
    TOTAL_REQUISITIONS("fn_detail_total_requisitions",
            List.of("Requisition ID","Department","Total No Of Positions","Vacancies")),
    TOTAL_DEPARTMENTS("fn_detail_total_departments",
            List.of("Department","Positions Count")),
    TOTAL_POSITIONS("fn_detail_total_positions",
            List.of("Requisition","Department","Position","Vacancies","Status")
    ),
    APPROVED_REQUISITIONS("fn_detail_approved_requisitions",
            List.of("Requisition","Total No Of Positions","Vacancies"),
            Set.of("department_name")),
    PENDING_REQUISITIONS("fn_detail_pending_requisitions",
            List.of("Requisition","Total No Of Positions","Vacancies"),
            Set.of("department_name")),
    ACTIVE_REQUISITIONS("fn_detail_active_requisitions",
            List.of("Requisition","Total No Of Positions","Vacancies"),
            Set.of("department_name")),
    CLOSED_REQUISITIONS("fn_detail_closed_requisitions",
            List.of("Requisition","Total No Of Positions","Vacancies"),
            Set.of("department_name")),
    CANDIDATE_PIPELINE_TOTAL_VACANCIES(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "interviews_completed", "interviews_scheduled",
                    "qualified_candidates", "applications_received",
                    "shortlisted_candidates"
            )
    ),

    CANDIDATE_PIPELINE_APPLICATIONS_RECEIVED(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "interviews_completed", "interviews_scheduled",
                    "qualified_candidates", "vacancy_count",
                    "shortlisted_candidates"
            )
    ),

    CANDIDATE_PIPELINE_SHORTLISTED_CANDIDATES(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "interviews_completed", "interviews_scheduled",
                    "qualified_candidates", "vacancy_count",
                    "applications_received"
            )
    ),

    CANDIDATE_PIPELINE_REJECTED_CANDIDATES(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "shortlisted_candidates",
                    "interviews_completed", "interviews_scheduled",
                    "qualified_candidates", "vacancy_count",
                    "applications_received"
            )
    ),

    CANDIDATE_PIPELINE_PENDING_CANDIDATES(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "rejected_candidates", "shortlisted_candidates",
                    "interviews_completed", "interviews_scheduled",
                    "qualified_candidates", "vacancy_count",
                    "applications_received"
            )
    ),

    CANDIDATE_PIPELINE_INTERVIEWS_SCHEDULED(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "shortlisted_candidates",
                    "interviews_completed",
                    "qualified_candidates", "vacancy_count",
                    "applications_received"
            )
    ),

    CANDIDATE_PIPELINE_INTERVIEWS_COMPLETED(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "shortlisted_candidates",
                    "interviews_scheduled",
                    "qualified_candidates", "vacancy_count",
                    "applications_received"
            )
    ),

    CANDIDATE_PIPELINE_QUALIFIED(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined", "offers_sent",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "shortlisted_candidates",
                    "interviews_scheduled",
                    "interviews_completed",
                    "vacancy_count", "applications_received"
            )
    ),

    CANDIDATE_PIPELINE_OFFERS_SENT(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined",
                    "offer_accepted", "offer_rejected",
                    "pending_candidates", "rejected_candidates",
                    "shortlisted_candidates",
                    "interviews_scheduled",
                    "interviews_completed",
                    "qualified_candidates",
                    "vacancy_count", "applications_received"
            )
    ),
    CANDIDATE_PIPELINE_OFFERS_ACCEPTED("fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
            "joined",
            "offers_sent",
            "offer_rejected",
            "pending_candidates", "rejected_candidates",
            "shortlisted_candidates",
            "interviews_scheduled",
            "interviews_completed",
            "qualified_candidates",
            "vacancy_count", "applications_received"
    )),
    CANDIDATE_PIPELINE_OFFERS_REJECTED(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "joined",
                    "offers_sent",
                    "offer_accepted",
                    "pending_candidates", "rejected_candidates",
                    "shortlisted_candidates",
                    "interviews_scheduled",
                    "interviews_completed",
                    "qualified_candidates",
                    "vacancy_count", "applications_received"
            )
    ),
    CANDIDATE_PIPELINE_JOINED(
            "fn_report_pipeline_details",
            List.of("Requisition ID","Department","Position","Count"),
            Set.of(
                    "offer_rejected",
                    "offers_sent",
                    "offer_accepted",
                    "pending_candidates", "rejected_candidates",
                    "shortlisted_candidates",
                    "interviews_scheduled",
                    "interviews_completed",
                    "qualified_candidates",
                    "vacancy_count", "applications_received"
            )
    ),
    COMMITTEE_INTERVIEW_PANEL("fn_report_interview_panel_performance",
            List.of("Name","Committee Type","Total No of Positions Assigned","No of Days"),
            Set.of("interviews_conducted","avg_turnaround_days","total_days_utilized")),
    COMMITTEE_SCREENING_PANEL("fn_report_screening_panel_performance",
            List.of("Name","Total No of Positions Assigned","No of Days"),
            Set.of("candidates_screened","avg_screening_days")),
    COMMITTEE_COMPENSATION_PANEL("fn_report_compensation_panel_performance",
            List.of("Name","Total No of Positions Assigned","No of Days"),
            Set.of("offers_reviewed","avg_approval_days")),
    STATE_WISE_DISTRIBUTION("fn_report_state_vacancy_dist",
            List.of("State","City","Total Vacancies","Filled Vacancies","Unfilled Vacancies","Fill Rate")),
    RECRUITER_PERFORMANCE_TABLE("fn_report_recruiter_performance",
            List.of(
                    "Requisition",
                    "Position",
                    "Vacancy",
                    "Applied",
                    "Shortlisted",
                    "Interview",
                    "Qualified",
                    "Offer Sent",
                    "Offer Accepted",
                    "Joined",
                    "Status"
            ),
            Set.of("recruiter_name")),

    //for new metric buttons
    TOTAL_VACANCIES_METRICS("fn_report_fy_cy_vacancies",
            List.of("Year","Employment Type","Grade","Position","Requisition Type","Department","Category","Vacancies","Filled Vacancies","Unfilled Vacancies"),
            Set.of("category_code")),
    TOTAL_ONGOING_VACANCIES_METRICS("fn_report_ongoing_vacancies",
            List.of()),
    TOTAL_CANDIDATES_JOINED_METRICS("fn_report_fy_cy_joined",
            List.of()),
    TOTAL_OFFER_METRICS("fn_report_fy_cy_offers",
            List.of("Year","Employment Type","Grade","Position","Requisition Type",
                    "Department","Candidate Category","Selected Category","Merit List Type"
                    ," Is Pwd","Total Offers Sent","Offer Accepted","Offers Rejected","Offers Pending"));

    private final String screenFunctionName;
    private final List<String> displayHeaders;
    private final Set<String> excludedHeaders;

    DashboardScreen(String functionName,List<String> displayHeaders) {
        this(functionName,displayHeaders,Set.of());   //
    }

    DashboardScreen(String functionName,List<String> displayHeaders, Set<String> excludedHeaders) {
        this.screenFunctionName = functionName;
        this.displayHeaders = displayHeaders;
        this.excludedHeaders = excludedHeaders;
    }



    @JsonCreator
    public static DashboardScreen fromValue(String value) {
        if (value == null || value.isBlank()) return null;

        for (DashboardScreen screen : values()) {
            if (screen.name().equalsIgnoreCase(value)) {
                return screen;
            }
        }
        // Throwing IllegalArgumentException allows Spring to see the root cause clearly
        throw new IllegalArgumentException("Invalid Requisition status.");
    }
}
