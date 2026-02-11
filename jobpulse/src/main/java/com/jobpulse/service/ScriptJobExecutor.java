package com.jobpulse.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
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
@ExecutorType(JobRequestDTO.JobType.SCRIPT)
@Slf4j
public class ScriptJobExecutor implements JobExecutor {

    private static final int DEFAULT_TIMEOUT = 300; 
    private static final int MAX_OUTPUT_LENGTH = 5000; 

    @Autowired
    private ObjectMapper mapper;
    
    @Override
    public void execute(Job job) {
        try {
            JsonNode node = mapper.readTree(job.getPayload());
            
            if (node.get("command") == null) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "Script payload must contain 'command' field");
            }
            
            String command = node.get("command").asText();
            int timeout = node.has("timeoutSeconds") ? node.get("timeoutSeconds").asInt() : DEFAULT_TIMEOUT;
            
            if (command.isBlank()) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "Command cannot be empty");
            }
            
            executeScript(job, command, timeout);
            
        } catch (NonRetryableJobException | RetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing script job {}: {}", job.getName(), e.getMessage());
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Failed to parse script payload: " + e.getMessage(), e);
        }
    }
    
    private void executeScript(Job job, String command, int timeout) {
        log.info("Executing script job {} with command: {}", job.getName(), maskSensitiveData(command));
        
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && output.length() < MAX_OUTPUT_LENGTH) {
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.warn("Error reading script output: {}", e.getMessage());
                }
            });
            outputThread.start();
            
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroy();
                log.error("Script job timeout after {} seconds", timeout);
                throw new RetryableJobException(JobFailureReason.TIMEOUT,
                    "Script execution timeout after " + timeout + " seconds");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Script failed with exit code {}", exitCode);
                String outputStr = output.toString();
                throw new NonRetryableJobException(JobFailureReason.UNKNOWN,
                    "Script failed with exit code " + exitCode + ". Output: " + outputStr);
            }
            
            log.info("Script job executed successfully: {} (Exit code: {})", job.getName(), exitCode);
            
        } catch (NonRetryableJobException | RetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error executing script: {}", e.getMessage(), e);
            throw new NonRetryableJobException(JobFailureReason.UNKNOWN,
                "Script execution failed: " + e.getMessage(), e);
        }
    }
    
    private String maskSensitiveData(String command) {
        return command.replaceAll("password[\\s]*=\\s*[^\\s]+", "password=***")
                      .replaceAll("token[\\s]*=\\s*[^\\s]+", "token=***")
                      .replaceAll("key[\\s]*=\\s*[^\\s]+", "key=***");
    }
}
