package com.bob.commonutil.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class CloudFlareTurnstileService {

    @Value("${cloudflare.turnstile.secret-key:default-key}")
    private String secretKey;

    @Value("${cloudflare.turnstile.verify-url:default-url}")
    private String verifyUrl;

    public boolean verify(String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", secretKey);
        map.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, request, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("success")) {
            log.info("Cloudflare verification success");
            return (Boolean) response.getBody().get("success");
        }

        log.warn("Cloudflare Verification Failed");
        return false;
    }
}
