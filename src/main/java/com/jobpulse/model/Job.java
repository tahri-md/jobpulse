package com.jobpulse.model;

import java.time.LocalDateTime;

import com.jobpulse.dto.request.JobRequestDTO.JobType;

import jakarta.persistence.Entity;
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
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;
    private String cronExpression;
    private boolean recurring;
    private JobType jobType;
    private String payload;
    @Enumerated(EnumType.STRING)
    private Status status;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRunTime;
    private String lastError;
}
