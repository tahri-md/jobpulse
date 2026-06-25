package com.jobpulse.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class DeadLetterJobResponse {
  private Long id;
  private String jobName;
  private String jobType;
  private String lastError;
  private LocalDateTime failedAt;
  private int retryCount;
  private int maxRetries;
}
