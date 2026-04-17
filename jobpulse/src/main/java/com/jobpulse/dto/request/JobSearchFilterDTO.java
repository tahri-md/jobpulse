package com.jobpulse.dto.request;

import java.time.LocalDateTime;

import com.jobpulse.model.Status;
import com.jobpulse.dto.request.JobRequestDTO.JobType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSearchFilterDTO {
    private String query; // Search by name
    private Status status; // Filter by status
    private JobType jobType; // Filter by type
    private LocalDateTime startDate; // Filter by date range
    private LocalDateTime endDate;
}
