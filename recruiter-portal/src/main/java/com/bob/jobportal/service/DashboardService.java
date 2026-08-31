package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.InvalidFileTypeException;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.enums.DashboardDateRangePreset;
import com.bob.db.enums.DashboardScreen;
import com.bob.db.model.DashBoardInputModel;
import com.bob.db.repository.DashboardRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private PdfConverterService pdfConverterService;


    public JsonNode getDashboardFilters() {
        try {
            JsonNode data = dashboardRepository.getDashboardFilters();
            validateData(data);
            return data;
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }


    public JsonNode getDashboardDetails(DashBoardInputModel model) {
        try {
            getFromDateAndToDateFromPresetRange(model);
            JsonNode data = dashboardRepository.getDashboardData(model);
            validateData(data);
            return data;
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }


    public byte[] getDashboardReport(DashBoardInputModel inputModel)
    {
        if (inputModel != null && inputModel.getReportScreen() == null) {

            throw new ManualValidationException("Invalid report download request");
        }

        if(inputModel!=null && inputModel.getExtension() == null){
            throw new InvalidFileTypeException("File type is not defined");
        }
        getFromDateAndToDateFromPresetRange(inputModel);
        String extension = inputModel.getExtension();
        List<String> displayHeaders = inputModel.getReportScreen().getDisplayHeaders();
        Set<String> excludedHeaders = inputModel.getReportScreen().getExcludedHeaders();
        try{
            JsonNode resultSet = dashboardRepository.findDashboardReportDetails(inputModel);
            if(inputModel.getReportScreen().equals(DashboardScreen.COMMITTEE_INTERVIEW_PANEL)) resultSet = filterByCommittee(resultSet, inputModel.getCommittee());
            validateData(resultSet);
            List<String> headers = extractHeadersFromJson(resultSet,excludedHeaders);
            if(displayHeaders==null || displayHeaders.isEmpty()){
                displayHeaders = headers.stream().map(this::buildDisplayHeader).toList();
            }

            if(AppConstants.DOWNLOAD_DOC_TYPE_PDF.equals(extension)){
                String screenName=null;
                if(inputModel.getReportScreen().equals(DashboardScreen.COMMITTEE_INTERVIEW_PANEL)) screenName=inputModel.getCommittee();
                else screenName=inputModel.getReportScreen().toString();
                byte[] pdfBytes = convertDataToPdf(resultSet,headers,displayHeaders,screenName);
                return pdfBytes;
            }else if(AppConstants.DOWNLOAD_DOC_TYPE_XLSX.equals(extension)){
                byte[] excelBytes = excelTemplateService.writeJsonNodeToExcel(resultSet,headers,displayHeaders);
                return excelBytes;
            }else{
                throw new InvalidFileTypeException("File format not supported");
            }
        }catch (JsonProcessingException e){
            throw new CommonException(e.getMessage());
        }
    }
    private JsonNode filterByCommittee(JsonNode resultSet, String committee) {
        if (committee == null || committee.isBlank()) {
            return resultSet;
        }
        if (!resultSet.isArray()) {
            return resultSet;
        }
        ArrayNode filtered = JsonNodeFactory.instance.arrayNode();
        for (JsonNode panel : resultSet) {
            JsonNode committeeNode = panel.get("committee_name");
            if (committeeNode != null &&
                    committee.equalsIgnoreCase(committeeNode.asText())) {
                filtered.add(panel);
            }
        }
        return filtered;
    }

    private byte[] convertDataToPdf(JsonNode result,List<String> headers,List<String> displayHeaders,String screenName){
        List<Map<String, String>> rows = new ArrayList<>();

        for (JsonNode node : result) {
            Map<String, String> map = new LinkedHashMap<>();

            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                map.put(field, node.path(field).asText(""));
            }

            rows.add(map);
        }

        Context context = new Context();
        context.setVariable("reportTitle",screenName.replaceAll("_"," ").toUpperCase()+" REPORT");
        context.setVariable("displayHeaders",displayHeaders);
        context.setVariable("headers",headers);
        context.setVariable("rows",rows);
        String html = templateEngine.process("report.html",context);
        byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(html);

        return pdfBytes;
    }

    private List<String> extractHeadersFromJson(JsonNode resultSet,Set<String> excludedHeaders){
        if (!resultSet.isArray()) {
            throw new ManualValidationException("Invalid  Request.Please Try again");
        }
        List<String> headers = new ArrayList<>();

        JsonNode first = resultSet.get(0);
        Iterator<String> fields = first.fieldNames();

        while (fields.hasNext()) {
            String field = fields.next();
            if(!excludedHeaders.contains(field)) {
                headers.add(field);
            }
        }
        return headers;
    }

    private String buildDisplayHeader(String header){
        String result = Arrays.stream(header.split("_"))
                .map(word -> word.isEmpty()
                        ? word
                        : Character.toUpperCase(word.charAt(0))
                        + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
        return result;
    }

    private void getFromDateAndToDateFromPresetRange(DashBoardInputModel inputModel){
        if(inputModel!=null && (inputModel.getDateRangePreset() == null || inputModel.getDateRangePreset() == DashboardDateRangePreset.CUSTOM)) {
            return;
        }
        validateDateRangePreset(inputModel);
        try {
            JsonNode datePreset = dashboardRepository.getDashboardDatePresets(inputModel);
            JsonNode data = datePreset.get("data");

            String fromDate = data.get("dateFrom").asText();
            String toDate = data.get("dateTo").asText();

            inputModel.setFromDate(LocalDate.parse(fromDate));
            inputModel.setToDate(LocalDate.parse(toDate));
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }

    }

    private void validateDateRangePreset(DashBoardInputModel inputModel) {

        DashboardDateRangePreset preset = inputModel.getDateRangePreset();



        switch (preset) {

            case FINANCIAL_YEAR:
                if (inputModel.getFyYear() == null ||
                        inputModel.getCyYear() != null ||
                        inputModel.getQuarter() != null ||
                        inputModel.getFromDate() != null ||
                        inputModel.getToDate() != null) {

                    throw new ManualValidationException(
                            "For Financial Year, only financial year must be present");
                }
                validateYear(inputModel.getFyYear());
                break;

            case CALENDAR_YEAR:
                if (inputModel.getCyYear() == null ||
                        inputModel.getFyYear() != null ||
                        inputModel.getQuarter() != null ||
                        inputModel.getFromDate() != null ||
                        inputModel.getToDate() != null) {

                    throw new ManualValidationException(
                            "For Calendar Year, only calendar year must be present");
                }
                validateYear(inputModel.getCyYear());
                break;

            case QUARTER:
                if (inputModel.getCyYear() == null ||
                        inputModel.getQuarter() == null ||
                        inputModel.getFyYear() != null ||
                        inputModel.getFromDate() != null ||
                        inputModel.getToDate() != null) {

                    throw new ManualValidationException(
                            "For Quarter, only calendar year and quarter must be present");
                }
                validateYear(inputModel.getCyYear());
                validateQuarter(inputModel.getQuarter());
                break;
            default:
                throw new ManualValidationException("Unsupported date range preset: " + preset);
        }
    }

    private void validateYear(Integer year) {
        if (year < 1000 || year > 9999) {
            throw new ManualValidationException("Year must be a valid 4-digit year");
        }
    }

    private void validateQuarter(Integer quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new ManualValidationException("Quarter must be between 1 and 4");
        }
    }

    private void validateData(JsonNode data){
        if (data.isEmpty()) {
            throw new ManualValidationException("No details found");
        }
    }

}
