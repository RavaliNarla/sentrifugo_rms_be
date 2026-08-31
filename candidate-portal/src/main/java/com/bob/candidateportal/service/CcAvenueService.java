package com.bob.candidateportal.service;

import com.bob.commonutil.enums.SmsTemplateType;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.SmsRequest;
import com.bob.commonutil.service.SmsService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.CcAvenueOrdersDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.ApplicationFeeCategoryCode;
import com.bob.db.enums.ApplicationPaymentStatus;
import com.bob.db.enums.CcAvenueOrderStatus;
import com.bob.db.mapper.CcAvenueOrdersMapper;
import com.bob.db.repository.*;
import com.ccavenue.security.AesCryptUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CcAvenueService { // Renamed from CcAvenueServiceImpl and removed interface implementation

    @Value("${ccavenue.url}")
    private String ccAvenueUrl;

    @Value("${ccavenue.accessCode}")
    private String accessCode;

    @Value("${ccavenue.merchantId}")
    private String merchantId;

    @Value("${ccavenue.redirect.url}")
    private String redirectUrl;

    @Value("${ccavenue.redirect.success.url}")
    private String successRedirectUrl;

    @Value("${ccavenue.redirect.failure.url}")
    private String failureRedirectUrl;

    @Value("${ccavenue.status.inquiry.url}")
    private String statusInquiryUrl;

    @Autowired
    private CcAvenueOrdersRepository ccAvenueOrdersRepository;

    @Autowired
    private CcAvenueOrdersMapper ccAvenueOrdersMapper;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private GenderMasterRepository genderMasterRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private ApplicationFeeRepository applicationFeeRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private SmsService smsService;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private PositionsRepository jobPositionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    private final AesCryptUtil aesCryptUtil;

    public CcAvenueService(@Value("${ccavenue.workingKey}") String workingKey) {
        this.aesCryptUtil = new AesCryptUtil(workingKey);
    }

    public Map<String, Object> initiatePayment(UUID applicationId, BigDecimal fee) {
            if(fee.compareTo(getApplicationFee()) !=0){
            throw new IllegalArgumentException("Request tampered! Fee not matching with application");
        }

        // 1. Create a record in the transactions table with status PENDING using builder.
        String orderId = UUID.randomUUID().toString(); // Generate orderId internally
        String currency = AppConstants.CCAVENUE_CURRENCY_INR; // Hardcoded for Indian Rupee

        CcAvenueOrdersEntity order = CcAvenueOrdersEntity.builder()
                .orderId(UUID.fromString(orderId))
                .amount(fee)
                .currency(currency)
                .status(CcAvenueOrderStatus.INITIATED)
                .applicationId(applicationId)
                .build();

        order = ccAvenueOrdersRepository.save(order);

        // 2. Build the parameter string.
        StringBuilder parameterString = new StringBuilder();
        parameterString.append(AppConstants.CCAVENUE_PARAM_MERCHANT_ID+"=").append(merchantId).append("&");
        parameterString.append(AppConstants.CCAVENUE_PARAM_ORDER_ID+"=").append(order.getOrderId()).append("&"); // Use UUID orderId
        parameterString.append(AppConstants.CCAVENUE_PARAM_AMOUNT+"=").append(fee).append("&");
        parameterString.append(AppConstants.CCAVENUE_PARAM_CURRENCY+"=").append(currency).append("&");
        parameterString.append(AppConstants.CCAVENUE_PARAM_REDIRECT_URL+"=").append(redirectUrl).append("&");
        parameterString.append(AppConstants.CCAVENUE_PARAM_CANCEL_URL+"=").append(redirectUrl);


        // 3. Encrypt the string using AesCryptUtil and your Working Key.
        String encRequest = aesCryptUtil.encrypt(parameterString.toString());

        // 4. Prepare parameters for the HTML form post.
        Map<String, String> ccavenueParams = new HashMap<>();
        ccavenueParams.put(AppConstants.CCAVENUE_PARAM_COMMAND, AppConstants.CCAVENUE_COMMAND_INITIATE_TRANSACTION);
        ccavenueParams.put(AppConstants.CCAVENUE_RESPONSE_ENC_REQUEST, encRequest);
        ccavenueParams.put(AppConstants.CCAVENUE_RESPONSE_ACCESS_CODE, accessCode);

        Map<String, Object> redirectData = new HashMap<>();
        redirectData.put(AppConstants.CCAVENUE_URL, ccAvenueUrl);
        redirectData.put(AppConstants.CCAVENUE_PARAMS, ccavenueParams);

        return redirectData;
    }

    public RedirectView handleCcAvenueCallback(String encResp) throws JsonMappingException {
        Map<String, String> transactionDetails = decryptAndParse(encResp);
        String orderIdStr = transactionDetails.get(AppConstants.CCAVENUE_PARAM_ORDER_ID);

        if (orderIdStr != null) {
            Optional<CcAvenueOrdersEntity> optionalOrder = ccAvenueOrdersRepository.findFirstByOrderIdOrderByCreatedDateDesc(UUID.fromString(orderIdStr));
            if (optionalOrder.isPresent()) {
                CcAvenueOrdersEntity updatedEntity = updateAndPersist(optionalOrder.get(), transactionDetails);
                if (CcAvenueOrderStatus.SUCCESS.equals(updatedEntity.getStatus())) {
                    return new RedirectView(successRedirectUrl);
                }
            } else {
                log.error("Order with ID {} not found for callback.", orderIdStr);
            }
        }
        return new RedirectView(failureRedirectUrl);
    }

    public ResponseEntity<String> handleCcAvenueWebhook(String encResp) {
        try {
            Map<String, String> transactionDetails = decryptAndParse(encResp);
            String orderIdStr = transactionDetails.get(AppConstants.CCAVENUE_PARAM_ORDER_ID);

            if (orderIdStr == null) {
                log.error("Webhook received with no order_id in payload.");
                return ResponseEntity.badRequest().body("Missing order_id");
            }

            Optional<CcAvenueOrdersEntity> optionalOrder = ccAvenueOrdersRepository.findFirstByOrderIdOrderByCreatedDateDesc(UUID.fromString(orderIdStr));
            if (optionalOrder.isEmpty()) {
                log.error("Webhook: Order with ID {} not found.", orderIdStr);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
            }

            CcAvenueOrdersEntity existingOrder = optionalOrder.get();

            // Idempotency guard — skip if already in a terminal SUCCESS state
            if (CcAvenueOrderStatus.SUCCESS.equals(existingOrder.getStatus())) {
                log.info("Webhook: Order {} already marked SUCCESS, skipping duplicate.", orderIdStr);
                return ResponseEntity.ok("Already processed");
            }

            updateAndPersist(existingOrder, transactionDetails);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            // Return 500 so CCAvenue retries the webhook delivery
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    private Map<String, String> decryptAndParse(String encResp) {
        String decryptedResponse = aesCryptUtil.decrypt(encResp);
        return Arrays.stream(decryptedResponse.split("&"))
                .map(s -> s.split("=", 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(
                        a -> URLDecoder.decode(a[0], StandardCharsets.UTF_8),
                        a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)
                ));
    }

    private CcAvenueOrdersEntity updateAndPersist(CcAvenueOrdersEntity existingOrder, Map<String, String> transactionDetails) throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CcAvenueOrdersDTO dto = mapper.updateValue(ccAvenueOrdersMapper.toDTO(existingOrder), transactionDetails);
        CcAvenueOrdersEntity updatedEntity = ccAvenueOrdersMapper.toEntity(dto);
        ccAvenueOrdersRepository.save(updatedEntity);

        UUID applicationId = existingOrder.getApplicationId();
        if (applicationId != null) {
            ApplicationPaymentStatus paymentStatus = CcAvenueOrderStatus.SUCCESS.equals(updatedEntity.getStatus())
                    ? ApplicationPaymentStatus.SUCCESS
                    : ApplicationPaymentStatus.FAILED;
            candidateApplicationsRepository.updatePaymentStatus(applicationId, paymentStatus.name());

            // Send SMS notification
            try {
                CandidateApplicationsEntity application = candidateApplicationsRepository.findByApplicationIdIgnoringPaymentStatus(applicationId)
                        .orElseThrow(() -> new RuntimeException("Application not found"));
                UUID candidateId = application.getCandidateId();

                CandidatesEntity candidate = candidatesRepository.findById(candidateId)
                        .orElseThrow(() -> new RuntimeException("Candidate not found"));

                JobPositionsEntity jobPosition = jobPositionsRepository.findById(application.getPositionId())
                        .orElseThrow(() -> new RuntimeException("Job position not found"));

                MasterPositionsEntity masterPosition = masterPositionsRepository.findById(jobPosition.getMasterPositionId())
                        .orElseThrow(() -> new RuntimeException("Master position not found"));

                SmsRequest smsRequest = SmsRequest.builder()
                        .phNumber(candidate.getMobileNumber())
                        .templateName(SmsTemplateType.JOBAPPLY)
                        .postingName(masterPosition.getPositionName())
                        .applicationId(application.getApplicationNo())
                        .build();
                smsService.sendSms(smsRequest);

            } catch (Exception e) {
                log.error("Failed to send SMS notification: {}", e.getMessage(), e);
            }

        }
        return updatedEntity;
    }

    public BigDecimal getApplicationFee() {
        UUID candidateId = securityUtils.getCurrentUserId();
        CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile not found"));

        GenderMasterEntity gender = genderMasterRepository.findById(candidateProfile.getGenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Gender not found"));
        ApplicationFeeCategoryCode categoryCode = null;

        ReservationCategoriesEntity reservationCategories = reservationCategoriesRepository.findById(candidateProfile.getReservationCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation Category not found"));

        if (candidateProfile.getExServiceman() != null ) {
            categoryCode = ApplicationFeeCategoryCode.ESM;
        } else if (candidateProfile.getDisability()) {
            categoryCode = ApplicationFeeCategoryCode.PWD;
        } else if (gender.getGender().equalsIgnoreCase(ApplicationFeeCategoryCode.WOMEN.getCode())) {
            categoryCode = ApplicationFeeCategoryCode.WOMEN;
        } else if (reservationCategories.getCategoryCode().equalsIgnoreCase(ApplicationFeeCategoryCode.SC.getCode())){
            categoryCode = ApplicationFeeCategoryCode.SC;
        } else if (reservationCategories.getCategoryCode().equalsIgnoreCase(ApplicationFeeCategoryCode.ST.getCode())){
            categoryCode = ApplicationFeeCategoryCode.ST;
        } else if (reservationCategories.getCategoryCode().equalsIgnoreCase(ApplicationFeeCategoryCode.GEN.getCode())) {
            categoryCode = ApplicationFeeCategoryCode.GEN;
        } else if (reservationCategories.getCategoryCode().equalsIgnoreCase(ApplicationFeeCategoryCode.EWS.getCode())) {
            categoryCode = ApplicationFeeCategoryCode.EWS;
        } else if (reservationCategories.getCategoryCode().equalsIgnoreCase(ApplicationFeeCategoryCode.OBC.getCode())) {
            categoryCode = ApplicationFeeCategoryCode.OBC;
        } else {
            categoryCode = ApplicationFeeCategoryCode.DEFAULT;
        }
        ApplicationFeeEntity applicationFee = applicationFeeRepository.findByCategoryCode(categoryCode);

        return applicationFee.getFeeAmount();
    }

   /* public ResponseEntity<Map<String, String>> getPaymentStatus(String orderId) {
        // 1. Build the parameter string for CCAvenue's status inquiry.
        StringBuilder statusParamString = new StringBuilder();
        statusParamString.append(AppConstants.CCAVENUE_PARAM_COMMAND).append("=").append(AppConstants.CCAVENUE_COMMAND_ORDER_STATUS_TRACKER).append("&");
        statusParamString.append(AppConstants.CCAVENUE_PARAM_MERCHANT_ID).append("=").append(merchantId).append("&");
        statusParamString.append(AppConstants.CCAVENUE_PARAM_ORDER_ID).append("=").append(orderId).append("&");
        statusParamString.append(AppConstants.CCAVENUE_PARAM_ACCESS_CODE).append("=").append(accessCode);

        // 2. Encrypt this parameter string using AesCryptUtil and Working Key.
        String encRequestForCcAvenue = aesCryptUtil.encrypt(statusParamString.toString());

        // 3. Prepare the request body for CCAvenue.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, String> requestBodyMap = new HashMap<>();
        requestBodyMap.put(AppConstants.CCAVENUE_REQUEST_ENC_REQUEST, encRequestForCcAvenue);
        requestBodyMap.put(AppConstants.CCAVENUE_RESPONSE_ACCESS_CODE, accessCode);

        String requestBody = requestBodyMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        // 4. Make an HTTP POST request to CCAvenue's status inquiry URL using WebClient.
        // Assuming 'webClient' is defined elsewhere or will be injected.
        // For now, I'll comment out the webClient part as it's not directly related to the AesCryptUtil refactoring.
        // Mono<String> ccAvenueResponseMono = webClient.post()
        //         .uri(statusInquiryUrl)
        //         .headers(httpHeaders -> httpHeaders.addAll(headers))
        //         .bodyValue(requestBody)
        //         .retrieve()
        //         .bodyToMono(String.class);

        // String encryptedCcAvenueResponse = ccAvenueResponseMono.block(); // Blocking for simplicity, consider reactive flow
        String encryptedCcAvenueResponse = ""; // Placeholder for now

        if (encryptedCcAvenueResponse == null || encryptedCcAvenueResponse.isEmpty()) {
            return new ResponseEntity<>(Map.of("error", "Failed to get status from CCAvenue"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 5. Decrypt the enc_response from CCAvenue.
        String decryptedCcAvenueResponse = aesCryptUtil.decrypt(encryptedCcAvenueResponse);

        // 6. Parse the decrypted response to extract transaction details.
        Map<String, String> statusDetails = Arrays.stream(decryptedCcAvenueResponse.split("&"))
                .map(s -> s.split("=", 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(
                        a -> URLDecoder.decode(a[0], StandardCharsets.UTF_8),
                        a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)
                ));

        String ccAvenueOrderId = statusDetails.get(AppConstants.CCAVENUE_PARAM_ORDER_ID);
        String ccAvenueTrackingId = statusDetails.get(AppConstants.CCAVENUE_PARAM_TRACKING_ID);
        String ccAvenueBankRefNo = statusDetails.get(AppConstants.CCAVENUE_PARAM_BANK_REF_NO);
        String ccAvenueOrderStatus = statusDetails.get(AppConstants.CCAVENUE_PARAM_ORDER_STATUS);

        // 7. Update the CcAvenueOrdersEntity in the database with the latest status and details.
        Optional<CcAvenueOrdersEntity> optionalOrder = ccAvenueOrdersRepository.findByTrackingId(orderId);

        if (optionalOrder.isPresent()) {
            CcAvenueOrdersEntity order = optionalOrder.get();
            order.setTrackingId(ccAvenueTrackingId != null ? ccAvenueTrackingId : order.getTrackingId());
            order.setBankRefNo(ccAvenueBankRefNo);
            order.setStatus(CcAvenueOrderStatus.fromCcAvenueStatus(ccAvenueOrderStatus)); // Use enum for status
            ccAvenueOrdersRepository.save(order);
        } else {
            log.error("Order with ID " + orderId + " not found in DB for status update.");
        }

        // 8. Return a JSON object containing the relevant status information.
        return new ResponseEntity<>(statusDetails, HttpStatus.OK);
    }*/
}
