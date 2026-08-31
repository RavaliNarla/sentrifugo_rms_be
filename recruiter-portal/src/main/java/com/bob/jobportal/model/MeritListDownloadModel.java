package com.bob.jobportal.model;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.entity.InterviewCentresEntity;
import com.bob.db.util.excel.ExcelDropdown;
import com.bob.db.util.excel.ExcelHeader;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MeritListDownloadModel {

    private UUID meritListId;

    private UUID offerId;

    @ExcelHeader("SNo")
    private Integer sNo;

    @ExcelHeader("Registration No")
    private String registrationNo;

    @ExcelHeader("Name")
    private String candidateName;

    @ExcelHeader("Caste")
    private String caste;

    @ExcelHeader("Offer Letter No")
    private String letterNo;

    @ExcelHeader(value = "State")
    private String state;

    @ExcelHeader(value ="City")
    private String city;

    @ExcelHeader("DOB")
    private String dob;

    @ExcelHeader("CutOff Date")
    private String cutOffDate;

    @ExcelHeader("Age")
    private String age;

    @ExcelHeader("Age Concession")
    private String ageConcession;

    @ExcelHeader("Disability")
    private String disability;

    @ExcelHeader("Disability Type")
    private String disabilityType;

    @ExcelHeader("Written Exam Marks Out of Total")
    private String writtenExamScore;

    @ExcelHeader("Written Exam Marks Out of Weightage")
    private String writtenExamScoreOutOfWeightage;

    @ExcelHeader("Marks Concession in Written Marks")
    private String examConcession;

    @ExcelHeader("Interview Score out of Total")
    private String interviewScore;

    @ExcelHeader("Interview Score out of Weightage")
    private String interviewScoreOutOfWeightage;

    @ExcelHeader("Marks Concession in Interview Score")
    private String interviewConcession;

    @ExcelHeader("Combined Score")
    private String combinedScore;

    @ExcelHeader("Q/NQ")
    private String qualifiedOrNot;

    @ExcelHeader("Shortlisted")
    private String shortlisted;

    @ExcelHeader("Select List")
    private String selectList;

    @ExcelHeader("Wait List")
    private String waitList;

    private UUID stateId;
    private UUID cityId;
    private UUID selectedReservationCategoryId;
    private UUID selectedDisabilityId;
    private Boolean selectedAgainstPwd;



}