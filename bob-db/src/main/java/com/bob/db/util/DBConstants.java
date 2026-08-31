package com.bob.db.util;

import java.util.List;

public class DBConstants {
    public static final String AUDIT_CHANGE_TYPE_CREATE = "CREATE";
    public static final String AUDIT_CHANGE_TYPE_UPDATE = "UPDATE";
    public static final String AUDIT_CHANGE_FIELD_ACTION = "ACTION";
    public static final String USER_ADMIN ="Admin";
    public static final String USERNAME="username";
    public static final String CANDIDATE_RESUME="Candidate_Resume_";
    public static final String CANDIDATE ="Candidate_";
    public static final String WORK_EXPERIENCE ="work_experience_";
    public static final String WORKFLOW_ACTION_UPDATE ="UPDATE";


    public static final String RESUME_FILE_CONTENT_LABEL = "Resume_File_Content: \n\n";

    public static final String AI_PROMPT_PARSE_RESUME = "Convert the above Resume_File_Content which was extracted using PDFTextStripper" +
            " into the JSON_SCHEMA which is provided below and follow the PARSE_RULES provided below very strictly";

    public static final String JSON_SCHEMA_LABEL = "\nJSON_SCHEMA: \n\n";

    public static final java.util.List<String> AI_PROMPT_PARSE_RESUME_RULES = java.util.Arrays.asList(
            "PARSE_RULES:\n\n",
            "- Use JSON_SCHEMA for parsing. Follow regex patterns exactly.\n",
            "- Extract only from resume text. No additions.\n",
            "- Experience Dates: DD-MM-YYYY format (use 01 if day missing, January (01) if month missing, but see STEP 0 for conflict resolution)\n",
            "- phone/mobile: can be same if one number found\n",
            "- skills: comma-separated list\n",
            "- currentEmployer/currentDesignation: from most recent job\n\n",
            "EXPERIENCE CALCULATION (CRITICAL) - FOLLOW EXACTLY:\n\n",
            "IMPORTANT: You MUST compute each job.totalYears first, then compute personal.totalExperience ONLY by summing those job.totalYears values.\n",
            "IMPORTANT: A literal line like 'TODAY_DATE: DD-MM-YYYY' will be present in the prompt. You MUST use THAT exact date value when endDate is Present/Current.\n\n",
            "STEP 0 - Normalize dates (apply BEFORE doing any math):\n",
            "- Every experience.startDate and experience.endDate MUST be DD-MM-YYYY.\n",
            "- If resume shows only MM-YYYY, set DD=01 by default. If only YYYY, set DD=01 and MM=01 (January).\n",
            "- CRITICAL DATE CONFLICT RESOLUTION: After setting default days, check for overlapping dates between consecutive jobs:\n",
            "  * If previous job endDate month-year matches next job startDate month-year (e.g., both are Oct 2024), set next job startDate day to 02.\n",
            "  * When only years given, if previous job endDate year matches next job startDate year (e.g., both are 2005), set next job startDate day to 02.\n",
            "  * This ensures no two jobs start and end on the exact same date.\n",
            "  * Example: Job1 ends '01-Oct-2024', Job2 starts 'Oct 2024' → Job2 becomes '02-Oct-2024'.\n",
            "  * Example: Job1 ends '01-Jan-2005', Job2 starts '2005' → Job2 becomes '02-Jan-2005'.\n",
            "- If endDate is Present/Current/Till date: set endDate to TODAY_DATE (from the prompt) in DD-MM-YYYY.\n",
            "- NEVER leave endDate empty for Present/Current. It MUST be a DD-MM-YYYY date.\n",
            "- If endDate is truly missing AND not implied as Present/Current, still set it to TODAY_DATE so duration can be computed.\n\n",
            "STEP 1 - Compute EACH job's duration in MONTHS using this exact rule:\n",
            "Let start = (sd, sm, sy), end = (ed, em, ey).\n",
            "months = (ey - sy)*12 + (em - sm)\n",
            "IF ed < sd then months = months - 1 else months = months\n",
            "IF months < 0 then months = 0\n",
            "Now convert months to totalYears format: years = months/12, remMonths = months%12, totalYears = 'X years Y months'. Even if result is some years and 0 months, include that '0 months' too.\n",
            "Do this for ALL jobs and fill experience[].totalYears.\n\n",
                "STEP 2 - Compute totalExperience STRICTLY from the per-job totals (DO NOT recalc from dates):\n",
                "- Convert EACH experience[].totalYears to months: jobMonths = (years*12 + months).\n",
                "- totalMonths = sum(all jobMonths).\n\n",
                "STEP 3 - Overlap correction (company-to-company SAME MONTH only):\n",
                "- Compare adjacent jobs in chronological order (oldest -> newest).\n",
                "- If previous job end month/year == next job start month/year, subtract 1 month from totalMonths for each such adjacent overlap.\n",
                "- totalMonths must not go below 0.\n\n",
                "STEP 4 - Set personal.totalExperience from totalMonths: 'X years Y months'.\n\n",
                "HARD VALIDATION (MUST PASS):\n",
                "- personal.totalExperience MUST equal the sum of experience[].totalYears (minus overlap corrections) exactly.\n",
                "- DO NOT copy total experience from summary text. ALWAYS compute it from the experience array you produced.\n\n",
            "CONCRETE EXAMPLE (must match):\n",
            "- If job totals are: 1 years 5 months, 3 years 0 months, 1 years 10 months\n",
            "  then totalMonths = (17 + 36 + 22) = 75 months = 6 years 3 months.\n\n",
            "STRICT RULES:\n",
            "- ALWAYS output totalYears and totalExperience in 'X years Y months' (never decimals like 2.5).\n",
            "- NEVER return null for totalYears or totalExperience; use '0 years 0 months' if needed.\n",
            "- Use \"\" (empty string) only for missing NON-numeric fields.\n\n",
                "EDUCATION (DO NOT DROP THIS):\n",
                "- Always include education key in output JSON. If nothing found, set education: []\n",
                "- If education is present in resume, include it in education array (at least level + school + passingYear when available).\n",
                "- Do not hallucinate education entries; only include those explicitly present.\n\n",
                "- Experience and Education arrays: order recent first\n",
            "- Return ONLY valid JSON (no markdown formatting)\n"
    );

    public static final java.util.List<String>  AI_PROMPT_PARSE_RESUME_RULES_FOR_EXISTING_DATA = java.util.Arrays.asList(
            "PARSE_RULES:\n\n",
            "- Use JSON_SCHEMA for parsing. Follow regex patterns exactly.\n",
            "- Extract only from resume text. No additions.\n",
            "- phone/mobile: can be same if one number found\n",
            "- skills: comma-separated list\n",
            "- Use \"\" (empty string) for missing fields\n",
            "- Return ONLY valid JSON (no markdown formatting)\n"
    );

    public static final java.util.List<String> AI_PROMPT_PARSE_RESUME_RULES_EXPERIENCE_ONLY = java.util.Arrays.asList(
            "PARSE_RULES:\n\n",
            "- Use JSON_SCHEMA for parsing. Follow regex patterns exactly.\n",
            "- Extract only from resume text. No additions.\n",
            "- Experience Dates: DD-MM-YYYY format (use 01 if day missing, January (01) if month missing, but see STEP 0 for conflict resolution)\n",
            "- phone/mobile: can be same if one number found\n",
            "- skills: comma-separated list\n",
            "- currentEmployer/currentDesignation: from most recent job\n\n",
            "EXPERIENCE CALCULATION (CRITICAL) - FOLLOW EXACTLY:\n\n",
            "IMPORTANT: You MUST compute each job.totalYears first, then compute personal.totalExperience ONLY by summing those job.totalYears values.\n",
            "IMPORTANT: A literal line like 'TODAY_DATE: DD-MM-YYYY' will be present in the prompt. You MUST use THAT exact date value when endDate is Present/Current.\n\n",
            "STEP 0 - Normalize dates (apply BEFORE doing any math):\n",
            "- Every experience.startDate and experience.endDate MUST be DD-MM-YYYY.\n",
            "- If resume shows only MM-YYYY, set DD=01 by default. If only YYYY, set DD=01 and MM=01 (January).\n",
            "- CRITICAL DATE CONFLICT RESOLUTION: After setting default days, check for overlapping dates between consecutive jobs:\n",
            "  * If previous job endDate month-year matches next job startDate month-year (e.g., both are Oct 2024), set next job startDate day to 02.\n",
            "  * When only years given, if previous job endDate year matches next job startDate year (e.g., both are 2005), set next job startDate day to 02.\n",
            "  * This ensures no two jobs start and end on the exact same date.\n",
            "  * Example: Job1 ends '01-Oct-2024', Job2 starts 'Oct 2024' → Job2 becomes '02-Oct-2024'.\n",
            "  * Example: Job1 ends '01-Jan-2005', Job2 starts '2005' → Job2 becomes '02-Jan-2005'.\n",
            "- If endDate is Present/Current/Till date: set endDate to TODAY_DATE (from the prompt) in DD-MM-YYYY.\n",
            "- NEVER leave endDate empty for Present/Current. It MUST be a DD-MM-YYYY date.\n",
            "- If endDate is truly missing AND not implied as Present/Current, still set it to TODAY_DATE so duration can be computed.\n\n",
            "STEP 1 - Compute EACH job's duration in MONTHS using this exact rule:\n",
            "Let start = (sd, sm, sy), end = (ed, em, ey).\n",
            "months = (ey - sy)*12 + (em - sm)\n",
            "IF ed < sd then months = months - 1 else months = months\n",
            "IF months < 0 then months = 0\n",
            "Now convert months to totalYears format: years = months/12, remMonths = months%12, totalYears = 'X years Y months'. Even if result is some years and 0 months, include that '0 months' too.\n",
            "Do this for ALL jobs and fill experience[].totalYears.\n\n",
            "STEP 2 - Compute totalExperience STRICTLY from the per-job totals (DO NOT recalc from dates):\n",
            "- Convert EACH experience[].totalYears to months: jobMonths = (years*12 + months).\n",
            "- totalMonths = sum(all jobMonths).\n\n",
            "STEP 3 - Overlap correction (company-to-company SAME MONTH only):\n",
            "- Compare adjacent jobs in chronological order (oldest -> newest).\n",
            "- If previous job end month/year == next job start month/year, subtract 1 month from totalMonths for each such adjacent overlap.\n",
            "- totalMonths must not go below 0.\n\n",
            "STEP 4 - Set personal.totalExperience from totalMonths: 'X years Y months'.\n\n",
            "HARD VALIDATION (MUST PASS):\n",
            "- personal.totalExperience MUST equal the sum of experience[].totalYears (minus overlap corrections) exactly.\n",
            "- DO NOT copy total experience from summary text. ALWAYS compute it from the experience array you produced.\n\n",
            "CONCRETE EXAMPLE (must match):\n",
            "- If job totals are: 1 years 5 months, 3 years 0 months, 1 years 10 months\n",
            "  then totalMonths = (17 + 36 + 22) = 75 months = 6 years 3 months.\n\n",
            "STRICT RULES:\n",
            "- ALWAYS output totalYears and totalExperience in 'X years Y months' (never decimals like 2.5).\n",
            "- NEVER return null for totalYears or totalExperience; use '0 years 0 months' if needed.\n",
            "- Use \"\" (empty string) only for missing NON-numeric fields.\n",
            "- Order experience array recent first\n",
            "- Return ONLY valid JSON (no markdown formatting)\n"
    );

    public static final java.util.List<String> AI_PROMPT_PARSE_RESUME_RULES_EDUCATION_ONLY = java.util.Arrays.asList(
            "PARSE_RULES:\n\n",
            "- Use JSON_SCHEMA for parsing. Follow regex patterns exactly.\n",
            "- Extract only from resume text. No additions.\n",
            "- phone/mobile: can be same if one number found\n",
            "- skills: comma-separated list\n",
            "- Use \"\" (empty string) for missing fields\n\n",
            "EDUCATION RULES:\n",
            "- Always include education key in output JSON. If nothing found, set education: []\n",
            "- If education is present in resume, include it in education array (at least level + school + passingYear when available)\n",
            "- Do not hallucinate education entries; only include those explicitly present\n",
            "- Order education array recent first\n",
            "- Return ONLY valid JSON (no markdown formatting)\n"
    );

    public static final String EXCEL_TEMPLATE_SUFFIX= "_template.xlsx";

    public static final String HEADER_CANDIDATE="candidate";
    public static final String HEDAER_RECRUITER="recruiter";
    public static final String HEADER_XCLIENT="X-Client";
    public static final String HEADER_AUTHORIZATION =  "Authorization";
    public static final String HEADER_BEARER =  "Bearer ";
    public static final String COOKIE_ACCESS_TOKEN="ACCESS_TOKEN";
    public static final String COOKIE_REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String REQUEST_ATTACHMENT="Candidate_Request_Attachment";

//    Master Constants
    public static final String GENERIC_DOCUMENT ="_info_v";
    public static final String MASTER_CREATED_DATE="createdDate";
    public static final String OTHER = "Other";

    public static final String CANDIDATE_MESSAGE_SUBJECT ="Message From " ;
    public static final String CANDIDATE_REQUEST_FORM_TEMPLATE = "CandidateRequestForm";
    public static final String VERIFICATION_STATUS = "verified";

    // candidate screening constants
    public static final String CANDIDATE_NAME = "candidateName";
    public static final String APPLICATION_NO = "applicationNo";
    public static final String POSITION_NAME = "positionName";
    public static final String DOCUMENTS_NAME = "documentList";
    public static final String DESCRANCY_LIST = "discrepancyList";

    public static final String DEADLINE_DATE = "deadlineDate";
    public static final String COMPANY_NAME = "companyName";
    public static final String DESCRANCY_EMAIL_TEMPLATE = "discrepancy-email.html";
    public static final String DESCRANCY_MESSAGE_SUBJECT = "Action Required: Document Discrepancy at Screening";
    public static final String DESCRANCY_WORK_EXPERIENCE = "Work Experience";
    public static final String DESCRANCY = "Submit Date Extension";
    public static final String JoiningDateExtension= "Joining Date Extension";
    public static final String ZONE_OFFICE_CHANGE_REQUEST ="Zone Office Change Request";

    public static final String SCREENING_COMMITTEE_NAME = "Screening";
    public static final String INTERVIEW_COMMITTEE_NAME = "Interview";
    public static final String COMPENSATION_COMMITTEE_NAME = "Compensation";
    public static final String HEADER_AZURE_AD = "AzureAD" ;

    public static final int EXCEL_MAX_CELL_LENGTH=32767;
    public static final int EXCEL_MIN_CELL_LENGTH=0;

    public static final List<String> JOB_REQUISITION_MONITORING_FILED_LIST = List.of(
            "requisitionStatus",
            "requisitionTitle",
            "requisitionDescription",
            "startDate",
            "endDate",
            "requisitionComments"
    );

    public static final String REQUISITION_TITLE = "requisitionTitle";
    public static final String REQUISITION_DESCRIPTION = "requisitionDescription";
    public static final String REQUISITION_COMMENTS = "requisitionComments";
    public static final String END_DATE = "endDate";
    public static final String START_DATE = "startDate";
    public static final String INTERVIEW_STATUS = "interviewStatus";
    public static final String REQUISITION_STATUS = "requisitionStatus";


}
