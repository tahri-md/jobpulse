package com.jobpulse.dto.request;


import java.time.LocalDateTime;
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

    private String name;

    private Long ownerId;

    private JobType jobType; // EMAIL, HTTP_CALL, DATA_CLEANUP, REPORT_GENERATION, etc.

    private String payload; // JSON string with job parameters

    private ScheduleDTO schedule;
    @Builder.Default
    private int maxRetries = 3; // default value

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

public enum JobType {
    EMAIL,
    HTTP_CALL,
    DATA_CLEANUP,
    REPORT_GENERATION
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {

    private ScheduleType type; // ONE_TIME, RECURRING, CRON

    // Only for ONE_TIME
    private LocalDateTime runAt;

    // Only for RECURRING
    private Frequency frequency; // MINUTES, HOURS, DAYS
    private Integer interval;    // every X minutes/hours/days

    // Only for CRON
    private String cronExpression; // optional advanced user
}
}




