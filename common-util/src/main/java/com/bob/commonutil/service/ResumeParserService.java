package com.bob.commonutil.service;

import com.bob.commonutil.model.candidateportal.ResumeModel;
import com.bob.db.repository.EducationRepository;
import com.bob.db.repository.WorkExperienceRepository;
import com.bob.db.util.DBConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ResumeParserService {
    @Autowired
    private FileService fileService;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    private final ObjectMapper objectMapper;

    public ResumeParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    private static final Pattern YEARS_MONTHS_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*years\\s*(\\d+)\\s*months\\s*$", Pattern.CASE_INSENSITIVE);

    public ResumeModel processResumeFileLLM(UUID candidateId, String filePath) {
        String fileContent = fileService.extractTextFromFile(filePath);
        if (fileContent == null || fileContent.isEmpty()) {
            log.warn("Could not extract content from PDF for file");
            return null; // FileService already logs errors, so just return null here.
        }

        // Check existing data in database
        boolean hasEducation = false;
        boolean hasExperience = false;
        
        if (candidateId != null) {
            try {
                hasEducation = !educationRepository.findByCandidateId(candidateId).isEmpty();
                hasExperience = !workExperienceRepository.findByCandidateId(candidateId).isEmpty();
                 } catch (Exception e) {
                log.warn("Error checking existing data for candidate ");
                // Continue with full parsing if DB check fails
            }
        }

        hasEducation = false;
        hasExperience = false;

        try { // This try-catch now covers the rest of the LLM processing and JSON parsing
            List<String> promptList = new ArrayList<>();
            String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-uuuu"));
            promptList.add("TODAY_DATE: " + todayDate + "\n");
            promptList.add(DBConstants.RESUME_FILE_CONTENT_LABEL);
            promptList.add(DBConstants.AI_PROMPT_PARSE_RESUME);
            fileContent=maskSensitiveData(fileContent);
            // Load and modify JSON schema based on existing data
            String resumeFormatJson = loadAndModifyResumeFormatJson(hasEducation, hasExperience);
            if (resumeFormatJson == null) {
                log.error("Failed to load resume_format.json for LLM processing.");
                return null;
            }
            promptList.add(DBConstants.JSON_SCHEMA_LABEL + resumeFormatJson);
            
            // Add skip instructions if needed
            if (hasEducation || hasExperience) {
                promptList.add(buildSkipInstructions(hasEducation, hasExperience));
            }
            
            // Add parsing rules conditionally based on what needs to be parsed
            promptList.addAll(getConditionalParsingRules(hasEducation, hasExperience));

            String openAiResult = openAIService.callOpenAIWithPromptList(promptList,fileContent,resumeFormatJson);
            // ObjectMapper objectMapper = new ObjectMapper(); // Removed this line

            // Extract the content field from the OpenAI result JSON
            String contentString = null;
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(openAiResult);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).path("message");
                    contentString = message.path("content").asText();
                }
            } catch (Exception ex) {
                log.error("Error extracting content from OpenAI result for file ");
                return null;
            }
            if (contentString == null || contentString.isEmpty()) {
                log.warn("OpenAI result content is null or empty for file");
                return null;
            }
            // Parse the content string into ResumeModel
            try {
                ResumeModel resumeModel = objectMapper.readValue(contentString, ResumeModel.class);
                
                // Ensure arrays are empty lists instead of null when not parsed
                if (resumeModel.getExperience() == null) {
                    resumeModel.setExperience(new ArrayList<>());
                }
                if (resumeModel.getEducation() == null) {
                    resumeModel.setEducation(new ArrayList<>());
                }
                if (resumeModel.getCertifications() == null) {
                    resumeModel.setCertifications(new ArrayList<>());
                }
                
                // Only calculate experience if we parsed experience array
                if (!hasExperience && !resumeModel.getExperience().isEmpty()) {
                    try {
                        applyTotalExperienceFromIndividualTotals(resumeModel);
                    } catch (Exception calcEx) {
                        log.warn("TotalExperience calculation failed for file ");
                        if (resumeModel != null && resumeModel.getPersonal() != null) {
                            resumeModel.getPersonal().setTotalExperience(null);
                        }
                    }
                } else {
                    log.info("Skipping experience calculation - candidate already has experience data");
                }
                
                return resumeModel;
            } catch (Exception ex) {
                log.error("Error parsing OpenAI content string to ResumeModel for file ");
                return null;
            }
        } catch (Exception e) {
            log.error("Unexpected error during LLM processing for file ");
            return null;
        }
    }
    private String maskSensitiveData(String text) {
        if (text == null) {
            return null;
        }

        // Mask email
        text = maskEmails(text);

        // Mask Indian phone numbers
        text = maskPhoneNumbers(text);

        text=maskPanNumbers(text);

        text=maskAadhaarNumbers(text);

        return text;
    }

    private String maskPhoneNumbers(String text) {
        Pattern pattern = Pattern.compile("(\\+91[-\\s]?)?([6-9]\\d{9})");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String prefix = matcher.group(1) == null ? "" : matcher.group(1);
            String phone = matcher.group(2);

            String maskedPhone = phone.charAt(0) + "********" + phone.charAt(phone.length() - 1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + maskedPhone));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskEmails(String text) {
        Pattern pattern = Pattern.compile("\\b([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String username = matcher.group(1);
            String domain = matcher.group(2);

            String maskedUsername;
            if (username.length() <= 2) {
                maskedUsername = username.charAt(0) + "*";
            } else {
                maskedUsername = username.charAt(0)
                        + "*".repeat(username.length() - 2)
                        + username.charAt(username.length() - 1);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(maskedUsername + "@" + domain));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskPanNumbers(String text) {
        Pattern pattern = Pattern.compile("\\b([A-Z]{5}\\d{4}[A-Z])\\b");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String pan = matcher.group(1);

            String masked = pan.charAt(0)
                    + "********"
                    + pan.charAt(pan.length() - 1);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskAadhaarNumbers(String text) {
        Pattern pattern = Pattern.compile("\\b(\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4})\\b");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String aadhaar = matcher.group(1);

            // Remove spaces/hyphens
            String digits = aadhaar.replaceAll("[\\s-]", "");

            String masked = digits.charAt(0)
                    + "**********"
                    + digits.charAt(digits.length() - 1);

            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Loads the resume_format.json resource from the classpath using two robust methods.
     * First tries ClassPathResource, then falls back to ClassLoader.getResourceAsStream.
     * Returns the file content as a String, or null if not found or error occurs.
     */
    private String loadResumeFormatJson() {
        try {
            // First attempt: ClassPathResource
            return new String(new ClassPathResource("resume_format.json").getInputStream().readAllBytes());
        } catch (Exception primaryEx) {
            log.warn("ClassPathResource failed to load resume_format.json, trying ClassLoader method: {}", primaryEx.getMessage());
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("resume_format.json");
                if (is != null) {
                    return new String(is.readAllBytes());
                } else {
                    log.error("ClassLoader failed to load resume_format.json: resource not found");
                    return null;
                }
            } catch (Exception secondaryEx) {
                log.error("Both resource loading methods failed for resume_format.json: ");
                return null;
            }
        }
    }

    /**
     * Loads and modifies the resume_format.json based on existing candidate data.
     * Removes education and/or experience arrays from the schema if candidate already has that data.
     */
    private String loadAndModifyResumeFormatJson(boolean hasEducation, boolean hasExperience) {
        String originalJson = loadResumeFormatJson();
        if (originalJson == null) {
            return null;
        }
        
        // If both arrays should be parsed, return original
        if (!hasEducation && !hasExperience) {
            return originalJson;
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(originalJson);
            ObjectNode propertiesNode = (ObjectNode) rootNode.path("properties");
            
            // Remove arrays based on existing data
            if (hasEducation) {
                propertiesNode.remove("education");
                log.info("Removed 'education' from JSON schema - candidate already has education data");
            }
            
            if (hasExperience) {
                propertiesNode.remove("experience");
                log.info("Removed 'experience' from JSON schema - candidate already has experience data");
            }
            
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("Error modifying resume_format.json: {}", e.getMessage());
            return originalJson; // Return original if modification fails
        }
    }

    /**
     * Builds skip instructions to inform GPT what arrays to skip.
     */
    private String buildSkipInstructions(boolean hasEducation, boolean hasExperience) {
        StringBuilder skipInstructions = new StringBuilder("\n\nSKIP INSTRUCTIONS:\n");
        
        if (hasEducation && hasExperience) {
            skipInstructions.append("- DO NOT parse 'education' array - candidate already has education records\n");
            skipInstructions.append("- DO NOT parse 'experience' array - candidate already has work experience records\n");
            skipInstructions.append("- Parse ONLY the 'personal' object with all its fields\n");
        } else if (hasEducation) {
            skipInstructions.append("- DO NOT parse 'education' array - candidate already has education records\n");
            skipInstructions.append("- Parse 'personal' object AND 'experience' array\n");
        } else if (hasExperience) {
            skipInstructions.append("- DO NOT parse 'experience' array - candidate already has work experience records\n");
            skipInstructions.append("- Parse 'personal' object AND 'education' array\n");
        }
        
        skipInstructions.append("- Return JSON with only the requested sections\n\n");
        return skipInstructions.toString();
    }

    /**
     * Returns parsing rules conditionally based on what data exists and what needs to be parsed.
     * 
     * @param hasEducation true if candidate already has education data in DB
     * @param hasExperience true if candidate already has experience data in DB
     * @return Appropriate rule set based on what needs to be parsed
     */
    private List<String> getConditionalParsingRules(boolean hasEducation, boolean hasExperience) {
        if (!hasEducation && !hasExperience) {
            // Parse both experience AND education - use full rules
            return DBConstants.AI_PROMPT_PARSE_RESUME_RULES;
        } else if (!hasExperience) {
            // Parse experience only (education exists) - use experience-specific rules with calculations
            return DBConstants.AI_PROMPT_PARSE_RESUME_RULES_EXPERIENCE_ONLY;
        } else if (!hasEducation) {
            // Parse education only (experience exists) - use education-specific rules
            return DBConstants.AI_PROMPT_PARSE_RESUME_RULES_EDUCATION_ONLY;
        } else {
            // Both exist, parse only personal data - use minimal rules
            return DBConstants.AI_PROMPT_PARSE_RESUME_RULES_FOR_EXISTING_DATA;
        }
    }

    /**
     * Computes personal.totalExperience by summing experience[].totalYears values.
     * Expected format per item: "X years Y months".
     * If calculation cannot be performed (missing/unparseable data), sets totalExperience to null.
     */
    private void applyTotalExperienceFromIndividualTotals(ResumeModel resumeModel) {
        if (resumeModel == null || resumeModel.getPersonal() == null) {
            return;
        }

        if (resumeModel.getExperience() == null || resumeModel.getExperience().isEmpty()) {
            resumeModel.getPersonal().setTotalExperience(null);
            return;
        }

        long totalMonths = 0;
        boolean sawAtLeastOne = false;

        for (var exp : resumeModel.getExperience()) {
            if (exp == null) {
                continue;
            }
            String totalYears = exp.getTotalYears();
            if (totalYears == null || totalYears.trim().isEmpty()) {
                // If any job exists but doesn't have a parsable duration, we treat the total as not reliably computable.
                resumeModel.getPersonal().setTotalExperience(null);
                return;
            }

            Matcher matcher = YEARS_MONTHS_PATTERN.matcher(totalYears.trim());
            if (!matcher.matches()) {
                resumeModel.getPersonal().setTotalExperience(null);
                return;
            }

            long years;
            long months;
            try {
                years = Long.parseLong(matcher.group(1));
                months = Long.parseLong(matcher.group(2));
            } catch (Exception parseEx) {
                resumeModel.getPersonal().setTotalExperience(null);
                return;
            }
            totalMonths += years * 12 + months;
            sawAtLeastOne = true;
        }

        if (!sawAtLeastOne) {
            resumeModel.getPersonal().setTotalExperience(null);
            return;
        }

        long years = totalMonths / 12;
        long months = totalMonths % 12;
        resumeModel.getPersonal().setTotalExperience(years + " years " + months + " months");
    }
}
