package com.jobpulse.dto.response;

import com.jobpulse.model.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class JobHistoryResponse {
  private Long id;
  private LocalDateTime runTime;
  private Status status;
  private String errorMessage;
  private int retryAttempt;
}
