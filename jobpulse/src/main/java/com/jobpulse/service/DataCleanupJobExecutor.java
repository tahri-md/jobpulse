package com.jobpulse.service;

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

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;


@Component
@ExecutorType(JobRequestDTO.JobType.DATA_CLEANUP)
@Slf4j
public class DataCleanupJobExecutor implements JobExecutor {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EntityManager entityManager;

    @Override
    public void execute(Job job) {
        try {
            JsonNode payload = mapper.readTree(job.getPayload());
            validatePayload(payload);
            
            String action = payload.get("action").asText();
            
            switch (action.toLowerCase()) {
                case "delete_old_records" -> deleteOldRecords(job, payload);
                case "truncate_table" -> truncateTable(job, payload);
                case "archive_data" -> archiveData(job, payload);
                default -> throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "Unknown cleanup action: " + action);
            }
            
            log.info("Data cleanup job executed successfully: {}", job.getName());
            
        } catch (NonRetryableJobException | RetryableJobException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing data cleanup job {}: {}", job.getName(), e.getMessage());
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Failed to parse cleanup payload: " + e.getMessage(), e);
        }
    }

    private void validatePayload(JsonNode payload) {
        if (payload.get("action") == null || payload.get("action").asText().isBlank()) {
            throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                "Cleanup payload must contain 'action' field");
        }
    }

    private void deleteOldRecords(Job job, JsonNode payload) {
        try {
            if (payload.get("tableName") == null || payload.get("tableName").asText().isBlank()) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "deleteOldRecords action requires 'tableName' field");
            }
            
            String tableName = payload.get("tableName").asText();
            String dateColumn = payload.has("dateColumn") ? payload.get("dateColumn").asText() : "created_at";
            int daysOld = payload.has("daysOld") ? payload.get("daysOld").asInt() : 30;
            
            String query = String.format(
                "DELETE FROM %s WHERE %s < NOW() - INTERVAL '%d days'",
                sanitizeTableName(tableName),
                sanitizeColumnName(dateColumn),
                daysOld
            );
            
            int deletedRows = entityManager.createNativeQuery(query).executeUpdate();
            log.info("Deleted {} old records from table {}", deletedRows, tableName);
            
        } catch (Exception e) {
            log.error("Error deleting old records: {}", e.getMessage());
            throw new RetryableJobException(JobFailureReason.UNKNOWN,
                "Failed to delete old records: " + e.getMessage(), e);
        }
    }

    private void truncateTable(Job job, JsonNode payload) {
        try {
            if (payload.get("tableName") == null || payload.get("tableName").asText().isBlank()) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "truncateTable action requires 'tableName' field");
            }
            
            String tableName = payload.get("tableName").asText();
            String query = String.format("TRUNCATE TABLE %s", sanitizeTableName(tableName));
            
            entityManager.createNativeQuery(query).executeUpdate();
            log.info("Truncated table: {}", tableName);
            
        } catch (Exception e) {
            log.error("Error truncating table: {}", e.getMessage());
            throw new RetryableJobException(JobFailureReason.UNKNOWN,
                "Failed to truncate table: " + e.getMessage(), e);
        }
    }

    private void archiveData(Job job, JsonNode payload) {
        try {
            if (payload.get("sourceTable") == null || payload.get("sourceTable").asText().isBlank()) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "archiveData action requires 'sourceTable' field");
            }
            if (payload.get("archiveTable") == null || payload.get("archiveTable").asText().isBlank()) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG,
                    "archiveData action requires 'archiveTable' field");
            }
            
            String sourceTable = payload.get("sourceTable").asText();
            String archiveTable = payload.get("archiveTable").asText();
            String dateColumn = payload.has("dateColumn") ? payload.get("dateColumn").asText() : "created_at";
            int daysOld = payload.has("daysOld") ? payload.get("daysOld").asInt() : 90;
            
            // Insert old records to archive table
            String insertQuery = String.format(
                "INSERT INTO %s SELECT * FROM %s WHERE %s < NOW() - INTERVAL '%d days'",
                sanitizeTableName(archiveTable),
                sanitizeTableName(sourceTable),
                sanitizeColumnName(dateColumn),
                daysOld
            );
            
            entityManager.createNativeQuery(insertQuery).executeUpdate();
            
            // Delete archived records from source
            String deleteQuery = String.format(
                "DELETE FROM %s WHERE %s < NOW() - INTERVAL '%d days'",
                sanitizeTableName(sourceTable),
                sanitizeColumnName(dateColumn),
                daysOld
            );
            
            int archivedRows = entityManager.createNativeQuery(deleteQuery).executeUpdate();
            log.info("Archived {} records to {}", archivedRows, archiveTable);
            
        } catch (Exception e) {
            log.error("Error archiving data: {}", e.getMessage());
            throw new RetryableJobException(JobFailureReason.UNKNOWN,
                "Failed to archive data: " + e.getMessage(), e);
        }
    }

    private String sanitizeTableName(String tableName) {
        // Basic validation - only allow alphanumeric and underscore
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        return tableName;
    }

    private String sanitizeColumnName(String columnName) {
        // Basic validation - only allow alphanumeric and underscore
        if (!columnName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid column name: " + columnName);
        }
        return columnName;
    }
}
