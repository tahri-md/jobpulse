package com.jobpulse.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class JobHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Job job; 

    private LocalDateTime runTime;
    @Enumerated(EnumType.STRING)
    private Status status;     
    private String errorMessage; 
    @Builder.Default
    private int retryAttempt = 0;   
}
