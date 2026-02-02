package com.jobpulse.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {

    @NotNull(message = "Schedule type is required")
    private ScheduleType type;

    // Only for ONE_TIME
    private LocalDateTime runAt;

    // Only for RECURRING
    private Frequency frequency;
    
    @Min(value = 1, message = "Interval must be at least 1")
    private Integer interval;

    // Only for CRON
    private String cronExpression;
       public enum ScheduleType {
    ONE_TIME,
    RECURRING,
    CRON
}

public enum Frequency {
    MINUTES,
    HOURS,
    DAYS
}
}
