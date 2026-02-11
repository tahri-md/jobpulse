package com.jobpulse.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.RetryableJobException;
import com.jobpulse.model.Job;
import com.jobpulse.model.User;

import lombok.extern.slf4j.Slf4j;

@Component
@ExecutorType(JobRequestDTO.JobType.EMAIL)
@Slf4j
public class EmailJobExecutor implements JobExecutor {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private GmailOAuthService gmailOAuthService;

    @Autowired
    private WebClient webClient;

    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    @Override
    public void execute(Job job) {
        try {
            JsonNode payload = mapper.readTree(job.getPayload());
            validateEmailPayload(payload);

            User owner = job.getOwner();

            if (owner == null || !hasGmailConnected(owner)) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                        "No Gmail account connected. Sign in with Google to connect your Gmail for sending emails.");
            }

            sendViaGmailApi(job, payload, owner);
            log.info("Email job executed successfully: {}", job.getName());

        } catch (NonRetryableJobException | RetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing email job {}: {}", job.getName(), e.getMessage());
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "Failed to process email job: " + e.getMessage(), e);
        }
    }

    // ───────────── Gmail API path ─────────────

    private boolean hasGmailConnected(User user) {
        return gmailOAuthService.getConnectionStatus(user).connected();
    }

    private void sendViaGmailApi(Job job, JsonNode payload, User owner) {
        try {
            String accessToken = gmailOAuthService.getValidAccessToken(owner);
            String rawEmail = buildRfc2822Message(payload, owner);
            String base64Url = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8));

            String requestBody = mapper.writeValueAsString(java.util.Map.of("raw", base64Url));

            String response = webClient.post()
                    .uri(GMAIL_SEND_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("Gmail API response: {}", response);
            log.info("Email sent via Gmail API to: {} (job: {})", payload.get("to").asText(), job.getName());

        } catch (NonRetryableJobException | RetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gmail API send failed for job {}: {}", job.getName(), e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("401")) {
                throw new RetryableJobException(JobFailureReason.AUTH_ERROR,
                        "Gmail access token may be invalid. Will retry with refreshed token.", e);
            }
            if (msg.contains("403")) {
                throw new NonRetryableJobException(JobFailureReason.AUTH_ERROR,
                        "Gmail API returned 403 Forbidden. Ensure the Gmail API is enabled in Google Cloud Console "
                        + "and the account has granted the gmail.send scope.", e);
            }
            throw new RetryableJobException(JobFailureReason.NETWORK_ERROR,
                    "Failed to send email via Gmail API: " + msg, e);
        }
    }

    /**
     * Builds a minimal RFC 2822 message that the Gmail API accepts as the "raw" field.
     */
    private String buildRfc2822Message(JsonNode payload, User owner) {
        GmailOAuthService.GmailTokenInfo info = gmailOAuthService.getConnectionStatus(owner);
        String from = info.gmailAddress();

        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(from).append("\r\n");
        sb.append("To: ").append(payload.get("to").asText()).append("\r\n");
        if (payload.has("cc") && !payload.get("cc").asText().isBlank()) {
            sb.append("Cc: ").append(payload.get("cc").asText()).append("\r\n");
        }
        sb.append("Subject: ").append(payload.get("subject").asText()).append("\r\n");
        sb.append("Content-Type: text/plain; charset=\"UTF-8\"\r\n");
        sb.append("\r\n");
        sb.append(payload.get("body").asText());
        return sb.toString();
    }

    // ───────────── Validation ─────────────

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
}
