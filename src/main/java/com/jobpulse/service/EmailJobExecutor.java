package com.jobpulse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.RetryableJobException;
import com.jobpulse.model.Job;

import lombok.extern.slf4j.Slf4j;

@Component
@ExecutorType(JobRequestDTO.JobType.EMAIL)
@Slf4j
public class EmailJobExecutor implements JobExecutor {

    @Autowired
    private ObjectMapper mapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public void execute(Job job) {
        try {
            if (mailSender == null) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "Email service is not configured");
            }

            JsonNode payload = mapper.readTree(job.getPayload());
            validateEmailPayload(payload);
            
            sendEmail(job, payload);
            log.info("Email job executed successfully: {}", job.getName());
            
        } catch (NonRetryableJobException | RetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing email job {}: {}", job.getName(), e.getMessage());
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Failed to parse email payload: " + e.getMessage(), e);
        }
    }

    private void validateEmailPayload(JsonNode payload) {
        if (payload.get("to") == null || payload.get("to").asText().isBlank()) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Email payload must contain 'to' field");
        }
        if (payload.get("subject") == null || payload.get("subject").asText().isBlank()) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Email payload must contain 'subject' field");
        }
        if (payload.get("body") == null || payload.get("body").asText().isBlank()) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Email payload must contain 'body' field");
        }
    }

    private void sendEmail(Job job, JsonNode payload) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            
            message.setTo(payload.get("to").asText());
            message.setSubject(payload.get("subject").asText());
            message.setText(payload.get("body").asText());
            
            if (payload.has("from") && !payload.get("from").asText().isBlank()) {
                message.setFrom(payload.get("from").asText());
            }
            
            if (payload.has("cc") && !payload.get("cc").asText().isBlank()) {
                message.setCc(payload.get("cc").asText());
            }
            
            mailSender.send(message);
            log.debug("Email sent to: {}", payload.get("to").asText());
            
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new RetryableJobException(JobFailureReason.NETWORK_ERROR,
                "Failed to send email: " + e.getMessage(), e);
        }
    }
}
