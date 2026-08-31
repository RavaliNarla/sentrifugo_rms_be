package com.bob.commonutil.service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MeetSchedulerService {
    @Value("${ms.graph.tenant-id}")
    private String tenantId;

    @Value("${ms.graph.client-id}")
    private String clientId;

    @Value("${ms.graph.client-secret}")
    private String clientSecret;

    @Value("${ms.graph.scope}")
    private String MS_GRAPH_SCOPE;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ms.graph.email}")
    private String userEmail;

    @Value("${ms.graph.url.access-token}")
    private String accessTokenUrlTemplate;

    @Value("${ms.graph.url.create-event}")
    private String createEventUrlTemplate;

    private String getAccessToken() {
        String url = accessTokenUrlTemplate.replace("{tenant-id}", tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&grant_type=client_credentials" +
                "&scope=" + MS_GRAPH_SCOPE;

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response == null || response.getBody() == null) {
            throw new ResourceNotFoundException("Online Meeting realted data not found.");
        }


        return (String) response.getBody().get("access_token") ;
    }


    public String createMeetingUrl(String subject,
                                   String startISO, String endISO) {
        String token = getAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("subject", subject);
        Map<String,String> start=new HashMap<>();
        start.put("dateTime",startISO);
        start.put("timeZone","Asia/Kolkata");
        body.put("start",start);
        Map<String,String> end=new HashMap<>();
        end.put("dateTime",endISO);
        end.put("timeZone","Asia/Kolkata");
        body.put("end",end);
        body.put("isOnlineMeeting",true);
        body.put("onlineMeetingProvider", "teamsForBusiness"); // Corrected typo here

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);


        String url = createEventUrlTemplate.replace("{user-email}", userEmail);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody != null && responseBody.containsKey("onlineMeeting")) {
            Map<String, Object> onlineMeetingInfo = (Map<String, Object>) responseBody.get("onlineMeeting");
            return (String) onlineMeetingInfo.get("joinUrl");
        }

        return "Join URL not found.";

    }
}

