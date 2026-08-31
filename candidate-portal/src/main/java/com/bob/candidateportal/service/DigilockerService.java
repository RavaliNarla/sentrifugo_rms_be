package com.bob.candidateportal.service;

import com.bob.candidateportal.model.DigilockerDataResponseModel;
import com.bob.candidateportal.util.DigilockerUtil;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.SchedulingConflictException;
import com.bob.commonutil.util.CustomMultipartFile;
import com.bob.commonutil.util.FileValidationUtil;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.EducationDTO;
import com.bob.db.entity.*;
import com.bob.db.mapper.EducationMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DigilockerService {

    @Autowired
    private DigilockerStateRepository digilockerStateRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CandidateDocumentsService candidateDocumentsService;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Value("${digilocker.client.id}")
    private String clientId;

    @Value("${digilocker.client.secret}")
    private String clientSecret;

    @Value("${digilocker.default.redirecturi}")
    private String redirectUri;

    @Value("${digilocker.base.url}")
    private String digilockerBaseUrl;

    @Value("${digilocker.auth.path}")
    private String digilockerAuthPath;

    @Value("${digilocker.token.path}")
    private String digilockerTokenPath;

    @Value("${digilocker.issued.path}")
    private String digilockerIssuedPath;

    @Value("${digilocker.uploaded.path}")
    private String digilockerUploadedPath;


    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private EducationMapper educationMapper;

    @Autowired
    private EducationQualificationsRepository educationQualificationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Transactional
    public String initiateAuthorization(String flowType, String scope, String docCode, Integer pageNo) throws NoSuchAlgorithmException {
        log.info("Initiating Digilocker authorization for flowType: {} and scope: {}", flowType, scope);
        String state = UUID.randomUUID().toString();
        String codeVerifier = DigilockerUtil.generateCodeVerifier();
        String codeChallenge = DigilockerUtil.generateCodeChallenge(codeVerifier);
        UUID candidateId = securityUtils.getCurrentUserId();
        log.debug("Generated state: {}, codeVerifier: {}, codeChallenge: {} for candidateId: {}", state, codeVerifier, codeChallenge, candidateId);
        List<DigilockerStateEntity> digilockerStateEntities = new ArrayList<>();
        if(pageNo==3 && docCode.equals("education")) {
            List<String> documentTypes=List.of("educationdocs");
            List<DocumentTypesEntity> documentTypesEntities=documentTypesRepository.findByDocTypeIn(documentTypes);
            List<String> docCodes=documentTypesEntities.stream().map(DocumentTypesEntity::getDocCode).toList();
            for (String indCode : docCodes) {
                DigilockerStateEntity digilockerStateEntity = DigilockerStateEntity.builder()
                        .state(state)
                        .codeVerifier(codeVerifier)
                        .flowType(flowType)
                        .candidateId(candidateId)
                        .docCode(indCode)
                        .pageNumber(pageNo)
                        .build();
                digilockerStateEntities.add(digilockerStateEntity);
            }
        }
        else {
            // Store state, verifier, flowType, and candidateId in the database
            DigilockerStateEntity digilockerStateEntity = DigilockerStateEntity.builder()
                    .state(state)
                    .codeVerifier(codeVerifier)
                    .flowType(flowType)
                    .candidateId(candidateId)
                    .docCode(docCode)
                    .pageNumber(pageNo)
                    .build();
            digilockerStateEntities.add(digilockerStateEntity);
        }
        digilockerStateRepository.saveAll(digilockerStateEntities);
        log.info("Saved Digilocker state to the database.");

        String authorizationUrl = getAuthorizationUrl(state, codeChallenge, scope);
        log.info("Generated Digilocker authorization URL: {}", authorizationUrl);
        return authorizationUrl;
    }

    @Transactional
    public List<DigilockerStateEntity> validateAndRetrieveState(String state) {
        log.info("Validating and retrieving state: {}", state);
        List<DigilockerStateEntity> digilockerStateEntities = digilockerStateRepository.findByState(state);
        if (!digilockerStateEntities.isEmpty()) {
            log.info("State found, deleting to prevent replay attacks.");
            // Immediately delete the state to prevent replay attacks
            digilockerStateRepository.deleteAll(digilockerStateEntities);
        } else {
            log.warn("Digilocker state not found for state: {}", state);
        }
        return digilockerStateEntities;
    }

    private String getAuthorizationUrl(String state, String codeChallenge, String scope) {
        String authUrl = digilockerBaseUrl + digilockerAuthPath;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authUrl)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256");

        if (StringUtils.hasText(scope)) {
            builder.queryParam("scope", scope);
        }

        return builder.build().toUriString();
    }

    public String getAccessToken(String code, String codeVerifier) {
        log.info("Requesting Digilocker access token with code and codeVerifier.");
        String tokenUrl = digilockerBaseUrl + digilockerTokenPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String requestBody = UriComponentsBuilder.newInstance()
                .queryParam("grant_type", "authorization_code")
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_verifier", codeVerifier)
                .build().encode().getQuery();

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        log.debug("Access token request body: {}", requestBody);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                entity,
                Map.class);

        String accessToken = (String) response.getBody().get("access_token");
        log.info("Successfully retrieved Digilocker access token.");
        log.debug("Access Token: {}", accessToken);
        return accessToken;
    }

    public Map<String, Object> getIssuedDocuments(String accessToken) {
        log.info("Fetching issued documents from Digilocker.");
        String issuedDocsUrl = digilockerBaseUrl + digilockerIssuedPath;
        return getDigilockerResource(accessToken, issuedDocsUrl);
    }

    public ResponseEntity<byte[]> fetchIssuedDocument(String accessToken, String uri) {
        log.info("Fetching issued document from Digilocker with URI: {}", uri);
        String fetchDocUrl = digilockerBaseUrl + "/oauth2/1/file/{uri}";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = null;
        try{
            response = restTemplate.exchange(
                    fetchDocUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class,
                    uri);
            log.info("Successfully fetched document from Digilocker.");
        }catch(Exception e){
            log.error("failed to fetch docuemnt from digilocker",e);
        }
        return response;
    }

    private String fetchIssuedDocumentXml(String accessToken, String uri) {
        log.info("Fetching issued document XML from Digilocker with URI: {}", uri);
        String fetchDocUrl = digilockerBaseUrl + "/oauth2/1/xml/{uri}";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = null;
        try{
            response = restTemplate.exchange(
                    fetchDocUrl,
                    HttpMethod.GET,
                    entity,
                    String.class,
                    uri);
            log.info("Successfully fetched document XML from Digilocker.");
        }catch(Exception e){
            log.error("failed to fetch document xml from digilocker",e);
        }

        return response != null ? response.getBody() : null;
    }

//    private String fetchIssuedDocumentJson(String accessToken,String uri){
//        log.info("Fetching issued document XML from Digilocker with URI: {}", uri);
//        String fetchDocUrl = digilockerBaseUrl + "/oauth2/1/xml/{uri}";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//        HttpEntity<?> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<String> response = null;
//        try{
//            response = restTemplate.exchange(
//                    fetchDocUrl,
//                    HttpMethod.GET,
//                    entity,
//                    String.class,
//                    uri);
//            log.info("Successfully fetched document XML from Digilocker.");
//        }catch(Exception e){
//            log.error("failed to fetch document xml from digilocker",e);
//        }
//
//        return response != null ? response.getBody() : null;
//    }

    private Map<String, Object> getDigilockerResource(String accessToken, String url) {
        log.info("Accessing Digilocker resource at URL: {}", url);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class);
        log.info("Successfully accessed Digilocker resource.");
        return response.getBody();
    }

    private String getReadableDocumentName(String docCode) {
        if (docCode == null) return "Document";
        switch (docCode.toUpperCase()) {
            case "PANCR": return "PAN Card";
            case "BOARD": return "10th / SSC Certificate";
            case "DRIVING_LICENCE": return "Driving License";
            case "BIRTH_CERT": return "Birth Certificate";
            case "COMMUNITY_CERT": return "Community Certificate";
            case "DISABILITY": return "Disability Certificate";
            case "INTER": return "Intermediate Certificate";
            case "DIPLOMA": return "Diploma Certificate";
            default: return "Requested Document";
        }
    }
    @Transactional
    public List<DigilockerDataResponseModel> processDigilockerCallback(String code, String state) throws Exception {
        log.info("Processing Digilocker callback for state: {}", state);

        List<DigilockerStateEntity> stateEntities = validateAndRetrieveState(state);
        if (stateEntities == null || stateEntities.isEmpty()) {
            throw new ManualValidationException("Invalid state parameter or state has expired.");
        }

        Integer pageNo= stateEntities.get(0).getPageNumber();
        String primaryDocCode= stateEntities.get(0).getDocCode();
        UUID candidateId= stateEntities.get(0).getCandidateId();

        // ── Batch prefetch ───────────────────────────────────────────────
        Set<String> neededDocCodes = stateEntities.stream()
                .map(DigilockerStateEntity::getDocCode)
                .collect(Collectors.toSet());

        List<DocumentTypesEntity> preFetchedDocTypes =
                documentTypesRepository.findByDocCodeIn(neededDocCodes);

        Map<String, DocumentTypesEntity> docTypeCache = preFetchedDocTypes.stream()
                .collect(Collectors.toMap(DocumentTypesEntity::getDocCode, d -> d));

        List<UUID> levelIds = preFetchedDocTypes.stream()
                .map(DocumentTypesEntity::getId).toList();

        Map<String, EducationQualificationsEntity> qualificationsMap =
                educationQualificationsRepository.findByLevelIdIn(levelIds).stream()
                        .collect(Collectors.toMap(
                                EducationQualificationsEntity::getQualificationName,
                                q -> q,
                                (existing, replacement) -> existing // <--- THE FIX: If there's a duplicate, keep the first one
                        ));

        // ── DigiLocker HTTP — once ───────────────────────────────────────
        String accessToken = getAccessToken(code, stateEntities.get(0).getCodeVerifier());

        Map<String, Object> issuedDocuments = getIssuedDocuments(accessToken);
        if (issuedDocuments == null || !(issuedDocuments.get("items") instanceof List)) {
            throw new ManualValidationException("No documents found in DigiLocker response.");
        }

        Map<String, String> documentUriMap = buildDocumentUriMap(issuedDocuments);

        // ── Loop ─────────────────────────────────────────────────────────
        List<DigilockerDataResponseModel> results     = new ArrayList<>();
        List<String>                      missingDocs = new ArrayList<>();
        int successfulUploads = 0;

        for (DigilockerStateEntity stateEntity : stateEntities) {
            String selectedDocCode= stateEntity.getDocCode();
            DocumentTypesEntity docType= docTypeCache.get(selectedDocCode);
            String uri                  = documentUriMap.get(selectedDocCode);

            if (uri == null) {
                log.warn("URI missing for docCode={}", selectedDocCode);
                missingDocs.add(getReadableDocumentName(selectedDocCode));
                results.add(buildResult(selectedDocCode, pageNo, false,
                        getReadableDocumentName(selectedDocCode) + " not found in DigiLocker"));
                continue;
            }

            try {
                String documentXml = fetchIssuedDocumentXml(accessToken, uri);
                if (documentXml == null) {
                    log.warn("Null XML for docCode={}", selectedDocCode);
                    missingDocs.add(getReadableDocumentName(selectedDocCode));
                    results.add(buildResult(selectedDocCode, pageNo, false, "Could not read document XML"));
                    continue;
                }

                Map<String, String> documentDetails =
                        extractDocumentDetailsFromXml(documentXml, selectedDocCode);

                ResponseEntity<byte[]> fileResponse = fetchIssuedDocument(accessToken, uri);
                if (fileResponse == null || fileResponse.getBody() == null) {
                    log.warn("Null file bytes for docCode={}", selectedDocCode);
                    results.add(buildResult(selectedDocCode, pageNo, false, "Could not fetch document file"));
                    continue;
                }

                String mimeType  = fileResponse.getHeaders().getContentType() != null
                        ? fileResponse.getHeaders().getContentType().toString() : "application/pdf";
                String extension = FileValidationUtil.getExtensionFromMimeType(mimeType);
                String fileName  = DBConstants.CANDIDATE + candidateId
                        + "_" + docType.getDocumentName() + "." + extension;
                MultipartFile multipartFile = new CustomMultipartFile(
                        fileResponse.getBody(), fileName, fileName, mimeType);

                uploadDocumentsFromDigilocker(
                        pageNo, candidateId, docType,
                        documentDetails, multipartFile, qualificationsMap);

                successfulUploads++;
                results.add(buildResult(selectedDocCode, pageNo, true, null));
                log.info("Successfully processed docCode={} for candidateId={}", selectedDocCode, candidateId);

            } catch (Exception ex) {
                log.error("Failed to process docCode={}", selectedDocCode, ex);
                missingDocs.add(getReadableDocumentName(selectedDocCode));
                results.add(buildResult(selectedDocCode, pageNo, false, "Processing failed: " + ex.getMessage()));
            }
        }

        // ── Outcome ──────────────────────────────────────────────────────
        // Only throw if EVERY document failed — partial success is still success
        if (successfulUploads == 0) {
            String errorMessage = "None of the expected documents were found. Please provide: "
                    + String.join(", ", missingDocs);
            throw new SchedulingConflictException(errorMessage,
                    DigilockerDataResponseModel.builder()
                            .docCode(primaryDocCode)
                            .pageNo(pageNo)
                            .isSuccess(false)
                            .build());
        }

        log.info("DigiLocker processing complete. success={}, failed={} for candidateId={}",
                successfulUploads, missingDocs.size(), candidateId);
        return results;
    }

    // Builds result entry cleanly — avoids repeating builder code in the loop
    private DigilockerDataResponseModel buildResult(
            String docCode, Integer pageNo, boolean isSuccess, String errorMessage) {
        return DigilockerDataResponseModel.builder()
                .docCode(docCode)
                .pageNo(pageNo)
                .isSuccess(isSuccess)
                .errorMessage(errorMessage)
                .build();
    }

    // Extracted from the loop — keeps buildDocumentUriMap separate
    private Map<String, String> buildDocumentUriMap(Map<String, Object> issuedDocuments) {
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) issuedDocuments.get("items");
        Map<String, String> uriMap = new HashMap<>();
        for (Map<String, Object> item : items) {
            String uri = (String) item.get("uri");
            if (uri == null || uri.isEmpty()) continue;
            if (uri.contains("-DRVLC-")){
                uriMap.put("DRIVING_LICENCE", uri);
            }
            else if (uri.contains("-PANCR-")){
                uriMap.put("PANCR", uri);
            }
            else if (uri.contains("-BTCER-")){
                uriMap.put("BIRTH_CERT", uri);
            }
            else if (uri.contains("-CDCER-") || uri.contains("-CTCER-")){
                uriMap.put("COMMUNITY_CERT", uri);
            }
            else if (uri.contains("-DPICR-")){
                uriMap.put("DISABILITY", uri);
            }
            else if (uri.contains("-HSCER-")){
                uriMap.put("INTER", uri);
            }
            else if (uri.contains("-DIPCR-")){
                uriMap.put("DIPLOMA", uri);
            }
            else if (uri.contains("-DGMST-")){
                uriMap.put("GRAD", uri);
            }
            else if (uri.contains("-SSCER-")) {
                uriMap.put("BOARD", uri);
                uriMap.put("TENTH", uri);
            }
            else if (uri.contains("-PGMST-") || uri.contains("-PGCR-")){
                uriMap.put("POST_GRAD", uri);
            }
            // PhD / Doctorate (Certificate & Marksheet)
            else if (uri.contains("-PHDCR-") || uri.contains("-PHDMS-")) {
                uriMap.put("PHD", uri);
            }
        }
        return uriMap;
    }

//    private Map<String, String> extractDocumentDetailsFromXml(String documentXml, String docCode) {
//        Map<String, String> docDetails = new HashMap<>();
//        if (documentXml == null || documentXml.isEmpty()) return docDetails;
//
//        switch (docCode) {
//            case "TENTH":{
//                docDetails.put("dob", extractValueFromXml(documentXml, "dob"));
//                break;
//            }
//            case "BOARD": {
//                docDetails.put("school",extractValueFromXml(documentXml, "schoolName"));
//                docDetails.put("board",extractValueFromXml(documentXml, "board"));
//                docDetails.put("year",extractValueFromXml(documentXml, "year"));
//                docDetails.put("gpaPoints",extractValueFromXml(documentXml, "gpaPoints"));
//                docDetails.put("percentage",extractValueFromXml(documentXml, "percentage")); // fallback
//                break;
//            }
//            case "INTER": {
//                docDetails.put("school",extractValueFromXml(documentXml, "schoolName"));
//                docDetails.put("board",extractValueFromXml(documentXml, "board"));
//                docDetails.put("year",extractValueFromXml(documentXml, "year"));
//                docDetails.put("gpaPoints",extractValueFromXml(documentXml, "gpaPoints"));
//                docDetails.put("percentage",extractValueFromXml(documentXml, "percentage"));
//                break;
//            }
//            case "DIPLOMA":
//            case "GRAD": {
//                docDetails.put("school",extractValueFromXml(documentXml, "collegeName"));
//                docDetails.put("university",extractValueFromXml(documentXml, "universityName"));
//                docDetails.put("year",extractValueFromXml(documentXml, "year"));
//                docDetails.put("gpaPoints",extractValueFromXml(documentXml, "gpaPoints"));
//                docDetails.put("percentage",extractValueFromXml(documentXml, "percentage"));
//                break;
//            }
//            case "PANCR": {
//                docDetails.put("documentNumber",extractValueFromXml(documentXml, "number"));
//                break;
//            }
//            case "DRIVING_LICENCE": {
//                docDetails.put("documentNumber",extractValueFromXml(documentXml, "number"));
//                break;
//            }
//        }
//        return docDetails;
//    }


    private Map<String, String> extractDocumentDetailsFromXml(String documentXml, String docCode) {
        Map<String, String> docDetails = new HashMap<>();
        if (documentXml == null || documentXml.isEmpty()) return docDetails;

        // Extract the Certificate Name for validation (e.g., "SECONDARY SCHOOL CERTIFICATE")
        docDetails.put("certificateName", extractTagAttribute(documentXml, "Certificate", "name"));

        switch (docCode) {
            case "TENTH":{
                String dob = extractTagAttribute(documentXml, "Person", "dob");
                if (dob.isEmpty()) dob = extractValueFromXml(documentXml, "dob");
                docDetails.put("dob", dob);
            }
            case "BOARD": {

                String school = extractTagAttribute(documentXml, "Person", "schoolName");
                if(school.isEmpty()) school=extractTagAttribute(documentXml,"School","name");
                if (school.isEmpty()) school = extractValueFromXml(documentXml, "schoolName");
                docDetails.put("school", school);

                String board = extractTagAttribute(documentXml, "Organization", "name");
                if (board.isEmpty()) board = extractValueFromXml(documentXml, "board");
                docDetails.put("board", board);

//                String year = extractTagAttribute(documentXml, "Examination", "year");
//                if (year.isEmpty()) year = extractValueFromXml(documentXml, "year");
//                docDetails.put("year", year);
//
//                String gpaPoints = extractTagAttribute(documentXml, "Performance", "cgpa");
//                if (gpaPoints.isEmpty()) gpaPoints = extractValueFromXml(documentXml, "gpaPoints");
//                docDetails.put("gpaPoints", gpaPoints);
//
//                String percentage = extractTagAttribute(documentXml, "Performance", "percentage");
//                if (percentage.isEmpty()) percentage = extractValueFromXml(documentXml, "percentage");
//                docDetails.put("percentage", percentage);

                break;
            }
            case "INTER": {
                String school = extractTagAttribute(documentXml, "School", "name");
                if (school.isEmpty()) school = extractValueFromXml(documentXml, "name");
                docDetails.put("school", school);

                String board = extractTagAttribute(documentXml, "Organization", "name");
                if (board.isEmpty()) board = extractValueFromXml(documentXml, "board");
                docDetails.put("board", board);

//                String year = extractTagAttribute(documentXml, "Examination", "year");
//                if (year.isEmpty()) year = extractValueFromXml(documentXml, "year");
//                docDetails.put("year", year);
//
//                String gpaPoints = extractTagAttribute(documentXml, "Performance", "cgpa");
//                if (gpaPoints.isEmpty()) gpaPoints = extractValueFromXml(documentXml, "gpaPoints");
//                docDetails.put("gpaPoints", gpaPoints);
//
//                String percentage = extractTagAttribute(documentXml, "Performance", "percentage");
//                if (percentage.isEmpty()) percentage = extractValueFromXml(documentXml, "percentage");
//                docDetails.put("percentage", percentage);

                break;
            }
            case "DIPLOMA":{
                String school = extractTagAttribute(documentXml, "School", "name");
                if (school.isEmpty()) school = extractValueFromXml(documentXml, "schoolName");
                docDetails.put("school", school);

                // They use <Course> for the actual diploma name (e.g., "DIPLOMA IN ENGINEERING")
                // We map this to the "board" variable so your resolver can catch it
                String board = extractTagAttribute(documentXml, "Course", "name");
                if (board.isEmpty()) board = extractValueFromXml(documentXml, "board");
                docDetails.put("board", board);

                break;
            }
            case "GRAD": {
                // Higher education usually uses <College> and <University> tags
                String school = extractTagAttribute(documentXml, "School", "name");
                docDetails.put("school", school);

                String board = extractTagAttribute(documentXml, "Course", "name");
                if (board.isEmpty()) board = extractValueFromXml(documentXml, "board");
                docDetails.put("board", board);

//                String courseName = extractTagAttribute(documentXml, "Course", "name");
//                docDetails.put("courseName", courseName);

                break;
            }
            case "PANCR": {
                String panNumber = extractTagAttribute(documentXml,"Certificate", "number");
                if (panNumber.isEmpty()) panNumber = extractValueFromXml(documentXml, "number");
                docDetails.put("documentNumber", panNumber);
                break;
            }
            case "DRIVING_LICENCE": {
                docDetails.put("documentNumber", extractTagAttribute(documentXml,"Certificate", "number"));
                break;
            }
            case "POST_GRAD":{
                // Higher education uses <School> for the College/University name
                String school = extractTagAttribute(documentXml, "School", "name");
                if (school.isEmpty()) school = extractValueFromXml(documentXml, "schoolName");
                docDetails.put("school", school);

                // They use <Course> for the actual degree name (e.g., "BACHELOR OF TECHNOLOGY")
                // We map this to the "board" variable so your resolveQualificationKey method catches it
                String board = extractTagAttribute(documentXml, "Course", "name");
                if (board.isEmpty()) board = extractValueFromXml(documentXml, "board");
                docDetails.put("board", board);

                break;
            }
            case "PHD": {
                // Higher education uses <School> for the College/University name
                String school = extractTagAttribute(documentXml, "School", "name");
                if (school.isEmpty()) school = extractValueFromXml(documentXml, "schoolName");
                docDetails.put("school", school);

                // They use <Course> for the actual degree name (e.g., "BACHELOR OF TECHNOLOGY")
                // We map this to the "board" variable so your resolveQualificationKey method catches it
                String board = extractTagAttribute(documentXml, "Course", "name");
                if (board.isEmpty()) board = extractValueFromXml(documentXml, "board");
                docDetails.put("board", board);

                break;
            }

        }
        return docDetails;
    }
    private String get10thQualificationNameByBoard(String board){
        board=board.toUpperCase();
        if (board.equals("BOARD OF SECONDARY EDUCATION")) {
            return "SSC";
        }
        if (board.equals("CENTRAL BOARD OF SECONDARY EDUCATION") || board.equals("CBSE")) {
            return "CBSE";
        }
        if (board.equals("COUNCIL FOR THE INDIAN SCHOOL CERTIFICATE") || board.equals("ICSE")) {
            return "ICSE";
        }
        if (board.equals("NATIONAL INSTITUTE OF OPEN SCHOOLING") || board.equals("NIOS")) {
            return "NIOS";
        }
        return null;
    }

    private String getInterQualificationNameByBoard(String board) {
        if (board == null || board.isBlank()) {
            return null;
        }
        String upperBoard = board.toUpperCase();

        if (upperBoard.equals("BOARD OF INTERMEDIATE EDUCATION")) {
            return "Intermediate Board";
        }
        //Need to check
        if (upperBoard.equals("CENTRAL BOARD OF SECONDARY EDUCATION") || upperBoard.equals("CBSE")) {
            return "CBSE (+2)";
        }
        if (upperBoard.equals("COUNCIL FOR THE INDIAN SCHOOL CERTIFICATE") || upperBoard.equals("ISC") || upperBoard.equals("CISCE")) {
            return "ICSE (+2)";
        }
        if (upperBoard.equals("NATIONAL INSTITUTE OF OPEN SCHOOLING") || upperBoard.equals("NIOS")) {
            return "NIOS 12th";
        }

        return null; // Default fallback
    }

    private String getDiplomaQualificationNameByBoard(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return "Diploma"; // Default fallback
        }

        String upperCourse = courseName.toUpperCase();

        // Add or adjust these strings to match your education_qualifications table exactly
        if (upperCourse.contains("DIPLOMA IN ENGINEERING") || upperCourse.contains("POLYTECHNIC") || upperCourse.contains("BTE")) {
            return "Diploma in Engineering";
        }
        if (upperCourse.contains("DIPLOMA IN PHARMACY") || upperCourse.contains("D.PHARM")) {
            return "Diploma in Pharmacy";
        }
        if (upperCourse.contains("DIPLOMA IN COMPUTER")) {
            return "Diploma in Computer Science";
        }

        return "Diploma"; // General fallback
    }

    private String getGraduationQualificationName(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return "Graduation"; // Default fallback
        }

        // .trim() removes any accidental leading/trailing spaces from the XML
        String upperCourse = courseName.trim().toUpperCase();

        if (upperCourse.equals("BACHELOR OF TECHNOLOGY") || upperCourse.equals("B.TECH") || upperCourse.equals("BTECH")) {
            return "Bachelor of Technology";
        }
        if (upperCourse.equals("BACHELOR OF ENGINEERING") || upperCourse.equals("B.E") || upperCourse.equals("B.E.")) {
            return "Bachelor of Engineering";
        }
        if (upperCourse.equals("BACHELOR OF SCIENCE") || upperCourse.equals("B.SC") || upperCourse.equals("BSC")) {
            return "Bachelor of Science";
        }
        if (upperCourse.equals("BACHELOR OF COMMERCE") || upperCourse.equals("B.COM") || upperCourse.equals("BCOM")) {
            return "Bachelor of Commerce";
        }
        if (upperCourse.equals("BACHELOR OF ARTS") || upperCourse.equals("B.A") || upperCourse.equals("B.A.")) {
            return "B.A";
        }
        if (upperCourse.equals("BACHELOR OF COMPUTER APPLICATIONS") || upperCourse.equals("BACHELOR OF COMPUTER APPLICATION") || upperCourse.equals("BCA") || upperCourse.equals("B.C.A")) {
            return "Bachelor of Computer Applications";
        }
        if (upperCourse.equals("BACHELOR OF MEDICINE & SURGERY") || upperCourse.equals("BACHELOR OF MEDICINE AND SURGERY") || upperCourse.equals("MBBS") || upperCourse.equals("M.B.B.S")) {
            return "Bachelor of Medicine & Surgery";
        }
        if (upperCourse.equals("BACHELOR OF DENTAL SURGERY") || upperCourse.equals("BDS") || upperCourse.equals("B.D.S")) {
            return "Bachelor of Dental Surgery";
        }
        if (upperCourse.equals("BACHELOR OF AYURVEDIC MEDICINE & SURGERY") || upperCourse.equals("BACHELOR OF AYURVEDIC MEDICINE AND SURGERY") || upperCourse.equals("BAMS") || upperCourse.equals("B.A.M.S")) {
            return "Bachelor of Ayurvedic Medicine & Surgery";
        }
        if (upperCourse.equals("BACHELOR OF EDUCATION") || upperCourse.equals("B.ED") || upperCourse.equals("BED")) {
            return "Bachelor of Education";
        }
        if (upperCourse.equals("BACHELOR OF ARCHITECTURE") || upperCourse.equals("B.ARCH") || upperCourse.equals("BARCH")) {
            return "Bachelor of Architecture";
        }

        return "Graduation"; // Fallback if it's a degree you haven't explicitly mapped
    }

    private void uploadDocumentsFromDigilocker(Integer pageNo,UUID candidateId,DocumentTypesEntity docType,Map<String,String> documentDataMap,MultipartFile multipartFile,Map<String,EducationQualificationsEntity> educationQualificationsEntityMap){
        if(pageNo==6){
            String documentNumber=documentDataMap.get("documentNumber");
            candidateDocumentsService.uploadIdProofDocument(candidateId, docType.getId(), documentNumber, false, null, multipartFile, true);
        }
        else if(pageNo==1){
            candidateDocumentsService.uploadDigilockerDocuments(candidateId,null,docType.getId(),multipartFile);
        }
        else if(pageNo==3){
            UUID educationId=saveBoardDocumentFromDigilocker(candidateId,documentDataMap,docType,educationQualificationsEntityMap);
            candidateDocumentsService.uploadDigilockerDocuments(candidateId,educationId,docType.getId(),multipartFile);
        }
    }


    private UUID saveBoardDocumentFromDigilocker(UUID candidateId, Map<String, String> documentDataMap,
                                                 DocumentTypesEntity documentType, Map<String, EducationQualificationsEntity> qualificationsMap) {

        String docCode = documentType.getDocCode();

        // 1. Resolve qualification
        String qualificationKey = resolveQualificationKey(docCode, documentDataMap.get("board"));
        EducationQualificationsEntity qualification = qualificationsMap.get(qualificationKey);

        if (qualification == null) {
            log.warn("No matching qualification for docCode={}, key={}", docCode, qualificationKey);
            return null;
        }

        // 2. Get Qualification IDs for this Level (Cleaned up Stream)
        List<UUID> filterQualificationIds = qualificationsMap.values().stream()
                .filter(qual -> qual.getLevelId().equals(documentType.getId()))
                .map(EducationQualificationsEntity::getId)
                .toList();

        // 3. Soft Delete Existing (With Safety Checks)
        if (!filterQualificationIds.isEmpty()) {
            educationRepository.softDeleteByCandidateIdAndQualificationIds(candidateId, filterQualificationIds);
            educationRepository.flush(); // FORCE Hibernate to execute delete before insert
        }

        // 4. Safe string extraction for Institution Name
        String schoolName = documentDataMap.get("school");
        if (schoolName == null || schoolName.isBlank()) {
            schoolName = "DigiLocker Certified Institution";
        }

        // 5. Build and Save
        EducationEntity education = EducationEntity.builder()
                .candidateId(candidateId)
                .educationQualificationsId(qualification.getId())
                .institutionName(schoolName)
                .build();

        UUID educationId = educationRepository.save(education).getId();
        log.info("Saved education for candidateId={}, qualification={}", candidateId, qualificationKey);

        return educationId;
    }

    private String resolveQualificationKey(String docCode, String boardName) {
        return switch (docCode) {
            case "BOARD", "TENTH" -> get10thQualificationNameByBoard(boardName != null ? boardName : "");
            case "INTER"-> getInterQualificationNameByBoard(boardName!=null?boardName:"");
            case "DIPLOMA"-> getDiplomaQualificationNameByBoard(boardName!=null?boardName:"");
            case "GRAD"-> getGraduationQualificationName(boardName!=null?boardName:"");
            case "POST_GRAD"      -> getPostGradQualificationName(boardName!=null?boardName:"");
            case "PHD"            -> getPhdQualificationName(boardName!=null?boardName:"");
            default               -> docCode;
        };
    }

    private String getPostGradQualificationName(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return "Post Graduation"; // Default fallback
        }

        // .trim() removes any accidental leading/trailing spaces from the XML
        String upperCourse = courseName.trim().toUpperCase();

        // Engineering & Technology
        if (upperCourse.contains("MASTER OF TECHNOLOGY") || upperCourse.contains("M.TECH") || upperCourse.contains("MTECH")) {
            return "Master of Technology";
        }
        if (upperCourse.contains("MASTER OF ENGINEERING") || upperCourse.contains("M.E.") || upperCourse.equals("ME") || upperCourse.equals("M.E")) {
            return "Master of Engineering";
        }

        // Business & Management
        if (upperCourse.contains("MASTER OF BUSINESS") || upperCourse.contains("MBA") || upperCourse.contains("M.B.A")) {
            return "Master of Business Administration";
        }

        // Computer Applications
        if (upperCourse.contains("MASTER OF COMPUTER") || upperCourse.contains("MCA") || upperCourse.contains("M.C.A")) {
            return "MCA";
        }

        // Science, Arts & Commerce
        if (upperCourse.contains("MASTER OF SCIENCE") || upperCourse.contains("M.SC") || upperCourse.contains("MSC")) {
            return "Master of Science";
        }
        if (upperCourse.contains("MASTER OF ARTS") || upperCourse.contains("M.A.") || upperCourse.equals("MA") || upperCourse.equals("M.A")) {
            return "Master of Arts";
        }
        if (upperCourse.contains("MASTER OF COMMERCE") || upperCourse.contains("M.COM") || upperCourse.contains("MCOM")) {
            return "Master of Commerce";
        }

        // Architecture & Education
        if (upperCourse.contains("MASTER OF ARCHITECTURE") || upperCourse.contains("M.ARCH") || upperCourse.equals("MARCH")) {
            return "Master of Architecture";
        }
        if (upperCourse.contains("MASTER OF EDUCATION") || upperCourse.contains("M.ED") || upperCourse.equals("MED")) {
            return "Master of Education";
        }

        // Law
        if (upperCourse.contains("MASTER OF LAW") || upperCourse.contains("LLM") || upperCourse.contains("L.L.M")) {
            return "Master of Laws";
        }

        // Medicine & Pharmacy
        if (upperCourse.contains("MASTER OF PHARMACY") || upperCourse.contains("M.PHARM") || upperCourse.contains("MPHARMA")) {
            return "Master of Pharmacy";
        }
        if (upperCourse.contains("MASTER OF SURGERY") || upperCourse.contains("M.S.") || upperCourse.equals("MS") || upperCourse.equals("M.S")) {
            return "Master of Surgery";
        }
        if (upperCourse.contains("DOCTOR OF MEDICINE") || upperCourse.contains("M.D.") || upperCourse.equals("MD") || upperCourse.equals("M.D")) {
            return "Doctor of Medicine";
        }

        return "Post Graduation"; // Fallback if it's a degree you haven't explicitly mapped
    }


    private String getPhdQualificationName(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            return "Ph.D"; // Default fallback
        }

        // .trim() removes any accidental leading/trailing spaces from the XML
        String upperCourse = courseName.trim().toUpperCase();

        // Engineering & IT
        // Placed above Science/Technology to ensure "Computer Science" or "Information Technology" gets routed here.
        if (upperCourse.contains("ENGINEERING") || upperCourse.contains("COMPUTER") || upperCourse.contains("INFORMATION TECHNOLOGY") || upperCourse.contains(" IT ")) {
            return "Engineering & IT PhD";
        }

        // Science & Technology
        if (upperCourse.contains("SCIENCE") || upperCourse.contains("TECHNOLOGY")) {
            return "Science & Technology PhD";
        }

        // Management & Commerce
        if (upperCourse.contains("MANAGEMENT") || upperCourse.contains("COMMERCE") || upperCourse.contains("BUSINESS") || upperCourse.contains("FINANCE") || upperCourse.contains("ACCOUNTING")) {
            return "Management & Commerce PhD";
        }

        // Arts & Humanities
        if (upperCourse.contains("ARTS") || upperCourse.contains("HUMANITIES") || upperCourse.contains("LITERATURE") || upperCourse.contains("HISTORY") || upperCourse.contains("SOCIOLOGY") || upperCourse.contains("PHILOSOPHY IN PHILOSOPHY")) {
            return "Arts & Humanities PhD";
        }

        // Agriculture & Allied Fields
        if (upperCourse.contains("AGRICULTURE") || upperCourse.contains("AGRONOMY") || upperCourse.contains("HORTICULTURE") || upperCourse.contains("VETERINARY")) {
            return "Agriculture & Allied Fields";
        }

        // Generic Fallback for generic certificates
        if (upperCourse.contains("DOCTOR OF PHILOSOPHY") || upperCourse.contains("PHD") || upperCourse.contains("PH.D")) {
            return "Ph.D";
        }

        return "Ph.D"; // Fallback if it's a degree branch you haven't explicitly mapped
    }



    private String extractDocumentNumberFromXml(String xml) throws Exception {
        log.debug("Extracting document number from XML.");
        String documentNumber = extractValueFromXml(xml, "pan");
        if (documentNumber.isEmpty()) {
            log.debug("'pan' attribute not found, falling back to 'number' attribute.");
            documentNumber = extractValueFromXml(xml, "number"); // Fallback
        }
        if (documentNumber.isEmpty()) {
            log.error("Could not extract document number from Digilocker document XML.");
        }
        return documentNumber;
    }

    private String extractValueFromXml(String xml, String attribute) {
        String tag = attribute + "=\"";
        int startIndex = xml.indexOf(tag);
        if (startIndex > -1) {
            startIndex += tag.length();
            int endIndex = xml.indexOf("\"", startIndex);
            if (endIndex > -1) {
                String value = xml.substring(startIndex, endIndex);
                log.debug("Extracted value '{}' for attribute '{}'", value, attribute);
                return value;
            }
        }
        log.debug("Attribute '{}' not found in XML.", attribute);
        return "";
    }
    private String extractTagAttribute(String xml, String tagName, String attribute) {
        if (xml == null || tagName == null || attribute == null) {
            return "";
        }

        try {
            // Regex Breakdown:
            // <tagName\b           -> Matches "<Certificate", "<Person", etc. (\b ensures exact word match)
            // [^>]*?               -> Lazily matches any characters inside the tag before our target attribute
            // \battribute\s*=\s* -> Matches our attribute followed by an equals sign (allows for spaces)
            // ["']([^"']*)["']     -> Matches the value inside either single or double quotes and captures it
            String regex = "<" + tagName + "\\b[^>]*?\\b" + attribute + "\\s*=\\s*[\"']([^\"']*)[\"']";

            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(xml);

            if (matcher.find()) {
                return matcher.group(1).trim(); // group(1) returns the value inside the quotes
            }
        } catch (Exception e) {
            log.debug("Regex extraction failed for tag: {}, attribute: {}", tagName, attribute);
        }

        return "";
    }
}
