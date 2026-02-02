package com.jobpulse.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class JobRequest {

    @NotBlank(message = "Job name is required")
    private String name;

    private String cronExpression;

    private boolean recurring;

    @Min(value = 0, message = "Retry count cannot be negative")
    private int retryCount;

    @NotNull(message = "Max retries is required")
    @Min(value = 0, message = "Max retries cannot be negative")
    private int maxRetries;

    private LocalDateTime nextRunTime;

    private String lastError;
}
