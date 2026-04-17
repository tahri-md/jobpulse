package com.jobpulse.dto.request;

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
public class JobTemplateRequestDTO {
    private String name;
    private String description;
    private JobType jobType;
    private String payload;
    private String cronExpression;
    private int maxRetries;
    private boolean isPublic;
}
