package com.jobpulse.service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.HttpJobPayload;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.RetryableJobException;
import com.jobpulse.model.Job;

import lombok.extern.slf4j.Slf4j;

@Component
@ExecutorType(JobRequestDTO.JobType.HTTP_CALL)
@Slf4j
public class HttpJobExecutor implements JobExecutor {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WebClient webClient;

    @Override
    public void execute(Job job) throws Exception {
        log.info("Executing HTTP job: {} (ID: {})", job.getName(), job.getId());
        
        HttpJobPayload payload = parsePayload(job.getPayload());
        validatePayload(payload);
        
        try {
            int timeoutSeconds = payload.getTimeoutSeconds() != null ? payload.getTimeoutSeconds() : 30;
            
            WebClient.RequestBodySpec request = webClient
                .method(HttpMethod.valueOf(payload.getMethod().toUpperCase()))
                .uri(payload.getUrl());

            if (payload.getHeaders() != null && !payload.getHeaders().isEmpty()) {
                request.headers(h -> h.setAll(payload.getHeaders()));
            }

            WebClient.ResponseSpec response =
                payload.getBody() != null
                    ? request.bodyValue(payload.getBody()).retrieve()
                    : request.retrieve();

            response
                .onStatus(HttpStatusCode::is5xxServerError, ClientResponse::createException)
                .onStatus(HttpStatusCode::is4xxClientError, ClientResponse::createException)
                .toBodilessEntity()
                .block(Duration.ofSeconds(timeoutSeconds));
                
            log.info("HTTP job executed successfully: {}", job.getName());
            
        } catch (WebClientResponseException e) {
            handleHttpError(e);
        } catch (WebClientRequestException  e) {
            log.error("Network error in HTTP job {}: {}", job.getName(), e.getMessage());
            throw new RetryableJobException(JobFailureReason.NETWORK_ERROR, 
                "Network error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error in HTTP job {}: {}", job.getName(), e.getMessage(), e);
            throw new NonRetryableJobException(JobFailureReason.UNKNOWN,
                "Unexpected error: " + e.getMessage(), e);
        }
    }
    
    private HttpJobPayload parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, HttpJobPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse HTTP payload: {}", e.getMessage());
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Invalid HTTP job payload: " + e.getMessage(), e);
        }
    }
    
    private void validatePayload(HttpJobPayload payload) {
        if (payload.getUrl() == null || payload.getUrl().isBlank()) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG, "URL cannot be null or empty");
        }
        if (payload.getMethod() == null || payload.getMethod().isBlank()) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG, "HTTP method cannot be null or empty");
        }
        try {
            HttpMethod.valueOf(payload.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG, 
                "Invalid HTTP method: " + payload.getMethod());
        }
    }
    
    private void handleHttpError(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        
        if (status >= 500) {
            log.warn("HTTP 5xx error in job: {}", e.getStatusCode());
            throw new RetryableJobException(JobFailureReason.REMOTE_5XX,
                "Remote server error (HTTP " + status + "): " + e.getStatusText(), e);
        } else if (status == 429) {
            log.warn("Rate limited (HTTP 429) in job");
            throw new RetryableJobException(JobFailureReason.RATE_LIMITED,
                "Rate limited by remote server", e);
        } else if (status == 401 || status == 403) {
            log.error("Authentication/Authorization error (HTTP {})", status);
            throw new NonRetryableJobException(JobFailureReason.AUTH_ERROR,
                "Authentication/Authorization failed (HTTP " + status + ")", e);
        } else if (status >= 400) {
            log.error("Client error (HTTP {}): {}", status, e.getStatusText());
            throw new NonRetryableJobException(JobFailureReason.BAD_REQUEST,
                "Bad request (HTTP " + status + "): " + e.getStatusText(), e);
        }
    }
}
