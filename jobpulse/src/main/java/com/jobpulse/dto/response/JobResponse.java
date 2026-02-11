package com.jobpulse.dto.response;

import java.time.LocalDateTime;

import com.jobpulse.model.Status;

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
public class JobResponse {
    private long id;
    private String name;
    private String jobType;
    private String payload;
    private String cronExpression;
    private boolean recurring;
    private Status status;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRunTime;
    private String lastError;
}
