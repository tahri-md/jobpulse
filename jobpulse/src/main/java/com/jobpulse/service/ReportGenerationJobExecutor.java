package com.jobpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.RetryableJobException;
import com.jobpulse.model.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ExecutorType(JobRequestDTO.JobType.REPORT_GENERATION)
@Slf4j
public class ReportGenerationJobExecutor implements JobExecutor {

  @Autowired private ObjectMapper mapper;

  @Autowired private EntityManager entityManager;

  @Override
  public void execute(Job job) {
    try {
      JsonNode payload = mapper.readTree(job.getPayload());
      validatePayload(payload);

      String reportType = payload.get("reportType").asText();
      String outputFormat =
          payload.has("outputFormat") ? payload.get("outputFormat").asText() : "CSV";

      switch (reportType.toLowerCase()) {
        case "user_activity" -> generateUserActivityReport(outputFormat);
        case "job_execution_stats" -> generateJobExecutionStats(outputFormat);
        case "system_health" -> generateSystemHealthReport(outputFormat);
        default ->
            throw new NonRetryableJobException(
                JobFailureReason.INVALID_CONFIG, "Unknown report type: " + reportType);
      }

      log.info("Report generation job executed successfully: {}", job.getName());

    } catch (NonRetryableJobException | RetryableJobException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error executing report generation job {}: {}", job.getName(), e.getMessage());
      throw new NonRetryableJobException(
          JobFailureReason.INVALID_CONFIG, "Failed to parse report payload: " + e.getMessage(), e);
    }
  }

  private void validatePayload(JsonNode payload) {
    if (payload.get("reportType") == null || payload.get("reportType").asText().isBlank()) {
      throw new NonRetryableJobException(
          JobFailureReason.INVALID_CONFIG, "Report payload must contain 'reportType' field");
    }
  }

  private void generateUserActivityReport(String outputFormat) {
    try {
      String reportPath = generateReport("user_activity", outputFormat);
      log.info("Generated user activity report: {}", reportPath);

    } catch (Exception e) {
      log.error("Error generating user activity report: {}", e.getMessage());
      throw new RetryableJobException(
          JobFailureReason.UNKNOWN,
          "Failed to generate user activity report: " + e.getMessage(),
          e);
    }
  }

  private void generateJobExecutionStats(String outputFormat) {
    try {
      String reportPath = generateReport("job_execution_stats", outputFormat);
      log.info("Generated job execution stats report: {}", reportPath);

    } catch (Exception e) {
      log.error("Error generating job execution stats: {}", e.getMessage());
      throw new RetryableJobException(
          JobFailureReason.UNKNOWN, "Failed to generate job execution stats: " + e.getMessage(), e);
    }
  }

  private void generateSystemHealthReport(String outputFormat) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

      StringBuilder report = new StringBuilder();
      report.append("System Health Report\n");
      report.append("Generated: ").append(timestamp).append("\n\n");

      // Get job stats
      String jobStats = "SELECT status, COUNT(*) as count FROM jobs GROUP BY status";
      Query jobQuery = entityManager.createNativeQuery(jobStats);
      var jobResults = jobQuery.getResultList();

      report.append("Job Status Distribution:\n");
      for (Object result : jobResults) {
        Object[] row = (Object[]) result;
        report.append("  ").append(row[0]).append(": ").append(row[1]).append("\n");
      }

      String reportPath = generateReport("system_health", outputFormat);
      log.info("Generated system health report: {}", reportPath);

    } catch (Exception e) {
      log.error("Error generating system health report: {}", e.getMessage());
      throw new RetryableJobException(
          JobFailureReason.UNKNOWN,
          "Failed to generate system health report: " + e.getMessage(),
          e);
    }
  }

  private String generateReport(String reportName, String format) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String filename = String.format("report_%s_%s.%s", reportName, timestamp, format.toLowerCase());

    log.debug("Report generated: {}", filename);

    return filename;
  }
}
