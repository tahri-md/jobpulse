package com.jobpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class JobStatsResponse {
  private long totalJobs;
  private long pendingJobs;
  private long runningJobs;
  private long successfulJobs;
  private long retryingJobs;
  private long failedJobs;
  private long deadLetterJobs;
}
