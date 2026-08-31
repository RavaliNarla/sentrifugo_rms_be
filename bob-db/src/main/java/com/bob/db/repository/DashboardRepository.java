package com.bob.db.repository;

import com.bob.db.model.DashBoardInputModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public DashboardRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode getDashboardData(DashBoardInputModel inputModel)
            throws JsonProcessingException {

        String sql = """
                SELECT gold.fn_dashboard_data(
                    :p_date_from,
                    :p_date_to,
                    :p_dept_id,
                    :p_position_id,
                    :p_zone,
                    :p_state_id,
                    :p_city_id,
                    :p_recruiter_id,
                    :p_employment_type_id,
                    :p_is_reinitialized
                )::text
                """;

        Query query = entityManager.createNativeQuery(sql);
        setDashboardParams(query, inputModel);

        return execute(query);
    }

    public JsonNode getDashboardFilters()
            throws JsonProcessingException {

        String sql = "SELECT gold.fn_dashboard_filters()::text";

        Query query = entityManager.createNativeQuery(sql);

        return execute(query);
    }

    public JsonNode getDashboardDatePresets(DashBoardInputModel inputModel)
            throws JsonProcessingException {

        String sql = """
                SELECT gold.fn_resolve_date_range_json(
                    :p_preset,
                    :p_date_from,
                    :p_date_to,
                    :p_fy_year,
                    :p_cy_year,
                    :p_quarter,
                    :p_half
                )::text
                """;

        Query query = entityManager.createNativeQuery(sql);
        setDatePresetParams(query, inputModel);

        return execute(query);
    }

    public JsonNode findDashboardReportDetails(DashBoardInputModel inputModel)
            throws JsonProcessingException {

        String procedureName =
                inputModel.getReportScreen().getScreenFunctionName();

        String sql = """
                SELECT json_agg(t)::text
                FROM gold.%s(
                :p_date_from,
                :p_date_to,
                :p_dept_id,
                :p_position_id,
                :p_zone,
                :p_state_id,
                :p_city_id,
                :p_recruiter_id,
                :p_employment_type_id,
                :p_is_reinitialized
            ) AS t
            """.formatted(procedureName);

        Query query = entityManager.createNativeQuery(sql);
        setDashboardParams(query, inputModel);

        return execute(query);
    }


    private JsonNode execute(Query query)
            throws JsonProcessingException {


        Object resultObj = query.getSingleResult();

        if (resultObj == null) {
            return objectMapper.createArrayNode();
        }

        String result = resultObj.toString();

        if (result.isBlank()) {
            return objectMapper.createArrayNode();
        }

        return objectMapper.readTree(result);
    }


    private void setDashboardParams(Query query, DashBoardInputModel inputModel) {

        query.setParameter("p_date_from", inputModel.getFromDate());
        query.setParameter("p_date_to", inputModel.getToDate());
        query.setParameter("p_dept_id", inputModel.getDepartmentId());
        query.setParameter("p_position_id", inputModel.getPositionId());
        query.setParameter("p_zone", inputModel.getZone());
        query.setParameter("p_state_id", inputModel.getStateId());
        query.setParameter("p_city_id", inputModel.getCityId());
        query.setParameter("p_recruiter_id", inputModel.getRecruiterId());
        query.setParameter("p_employment_type_id", inputModel.getEmploymentTypeId());
        query.setParameter("p_is_reinitialized", inputModel.getIsReinitialized());
    }


    private void setDatePresetParams(Query query, DashBoardInputModel inputModel) {

        query.setParameter(
                "p_preset", inputModel.getDateRangePreset() != null
                        ? inputModel.getDateRangePreset().toString() : null);
        query.setParameter("p_date_from", inputModel.getFromDate());
        query.setParameter("p_date_to", inputModel.getToDate());
        query.setParameter("p_fy_year", inputModel.getFyYear());
        query.setParameter("p_cy_year", inputModel.getCyYear());
        query.setParameter("p_quarter", inputModel.getQuarter());
        query.setParameter("p_half", null);
    }
}