package com.jobpulse.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequestDTO {

    @NotBlank(message = "Job name is required")
    private String name;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    private String payload;

    @Valid
    @NotNull(message = "Schedule is required")
    private ScheduleDTO schedule;

    @Min(value = 0, message = "Max retries cannot be negative")
    @Builder.Default
    private int maxRetries = 3;

 

public enum JobType {
    EMAIL,
    HTTP_CALL,
    DATA_CLEANUP,
    REPORT_GENERATION,
    SCRIPT,
    LOG
}

}
