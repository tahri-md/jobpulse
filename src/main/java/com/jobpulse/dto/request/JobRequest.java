package com.jobpulse.dto.request;

import java.time.LocalDateTime;

import com.jobpulse.model.Status;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    private String name;
    private String cronExpression;
    private boolean recurring;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRunTime;
    private String lastError;
}
