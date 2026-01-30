package com.jobpulse.dto.response;

import java.time.LocalDateTime;

import com.jobpulse.dto.response.AuthResponse.UserResponse;
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
public class JobResponse {
    private String name;
    private UserResponse owner;
    private String cronExpression;
    private boolean recurring;
    private Status status;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRunTime;
    private String lastError;
}
