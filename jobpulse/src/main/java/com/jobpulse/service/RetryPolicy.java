package com.jobpulse.service;


import java.util.Map;

import org.springframework.stereotype.Component;

import com.jobpulse.dto.others.JobFailureReason;


@Component
public class RetryPolicy {

    private final Map<JobFailureReason, Boolean> policy = Map.of(
        JobFailureReason.NETWORK_ERROR, true,
        JobFailureReason.TIMEOUT, true,
        JobFailureReason.RATE_LIMITED, true,
        JobFailureReason.REMOTE_5XX, true,

        JobFailureReason.BAD_REQUEST, false,
        JobFailureReason.AUTH_ERROR, false,
        JobFailureReason.INVALID_CONFIG, false,
        JobFailureReason.UNKNOWN, false
    );

    public boolean isRetryable(JobFailureReason reason) {
        return policy.getOrDefault(reason, false);
    }
}
