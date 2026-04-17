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
import org.springframework.data.annotation.CreatedDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class JobTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    private String name;
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;
    
    @Enumerated(EnumType.STRING)
    private JobType jobType;
    
    private String payload;
    private String cronExpression;
    private int maxRetries;
    
    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private boolean isPublic;
}
