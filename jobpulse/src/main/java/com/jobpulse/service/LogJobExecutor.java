package com.jobpulse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.model.Job;

import lombok.extern.slf4j.Slf4j;

@Component
@ExecutorType(JobRequestDTO.JobType.LOG)
@Slf4j
public class LogJobExecutor implements JobExecutor {

    @Autowired
    private ObjectMapper mapper;
    
    @Override
    public void execute(Job job) {
        try {
            JsonNode node = mapper.readTree(job.getPayload());
            
            if (node.get("message") == null) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "Log payload must contain 'message' field");
            }
            
            String message = node.get("message").asText();
            String level = node.has("level") ? node.get("level").asText().toUpperCase() : "INFO";
            
            logMessage(level, message);
            log.info("Log job executed successfully: {}", job.getName());
            
        } catch (NonRetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing log job {}: {}", job.getName(), e.getMessage());
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Failed to parse log payload: " + e.getMessage(), e);
        }
    }
    
    private void logMessage(String level, String message) {
        switch (level) {
            case "DEBUG" -> log.debug("[JOB] {}", message);
            case "INFO" -> log.info("[JOB] {}", message);
            case "WARN" -> log.warn("[JOB] {}", message);
            case "ERROR" -> log.error("[JOB] {}", message);
            default -> log.info("[JOB] {}", message);
        }
    }
}
