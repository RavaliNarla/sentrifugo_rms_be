package com.bob.commonutil.service;

import com.bob.commonutil.util.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@Service
public class OpenAIService {
    @Value("${openai.api.url}")
    private String openAIUrl;
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4.1}")
    private String openAIModel;

    @Value("${openai.embedding.url}")
    private String embeddingUrl;

    @Value("${openai.embedding.model}")
    private String embeddingModel;

    static class OpenAIMessage {
        public String role;
        public String content;
        public OpenAIMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class OpenAIRequest {
        public String model;
        public java.util.List<OpenAIMessage> messages;
        public OpenAIRequest(String model, java.util.List<OpenAIMessage> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    /**
     * Calls OpenAI LLM with a string prompt and returns the JSON response.
     * @param prompt The prompt to send to OpenAI.
     * @return JSON response from OpenAI.
     */
    public String callOpenAIWithPrompt(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            OpenAIMessage message = new OpenAIMessage("user", prompt);
            OpenAIRequest request = new OpenAIRequest(openAIModel, Collections.singletonList(message));
            String requestBody = objectMapper.writeValueAsString(request);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(openAIUrl, HttpMethod.POST, entity, String.class);
            return response.getBody(); // JSON response from OpenAI
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Calls OpenAI LLM with a list of prompt strings, returns the JSON response.
     * Concatenates into single message for faster processing.
     * @param prompts List of prompt strings to send to OpenAI.
     * @return JSON response from OpenAI.
     */
    public String callOpenAIWithPromptList(List<String> prompts,String resumeText,String resumeFormatJson) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Concatenate all prompts into single message for faster processing
            StringBuilder concatenatedPrompt = new StringBuilder();
            for (String prompt : prompts) {
                concatenatedPrompt.append(prompt);
            }
            String safeResume = AppConstants.OPENAI_INSTRUCTION_PROMPT +
                    "<<<RESUME_START>>>\n"
                    +resumeText+
                    "\n<<<RESUME_END>>>";

            OpenAIMessage systemPrompt = new OpenAIMessage("system", concatenatedPrompt.toString());
            OpenAIMessage resumeData =  new OpenAIMessage("user",safeResume);
            OpenAIMessage resumeFormat = new OpenAIMessage("user",resumeFormatJson);

            OpenAIRequest request = new OpenAIRequest(openAIModel, List.of(systemPrompt,resumeData,resumeFormat));
            String requestBody = objectMapper.writeValueAsString(request);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(openAIUrl, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "{\"error\": \" openai call failed \"}";
        }
    }


    static class EmbeddingRequest {
        public String model;
        public String input;

        public EmbeddingRequest(String model, String input) {
            this.model = model;
            this.input = input;
        }
    }

    public float[] generateEmbedding(String text) {

        if (text == null || text.isBlank()){
            return null;
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            EmbeddingRequest request = new EmbeddingRequest(embeddingModel, text);

            String requestBody = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(embeddingUrl, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.path("data").get(0).path("embedding");

            float[] embedding = new float[embeddingNode.size()];
            int i = 0;
            for (JsonNode value : embeddingNode) {
                embedding[i++] = (float) value.asDouble();
            }

            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Embedding error: " + e.getMessage());
        }
    }
}
