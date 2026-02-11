package com.jobpulse.exception;

import com.jobpulse.dto.others.JobFailureReason;

public abstract class JobExecutionException extends RuntimeException {

    private final JobFailureReason reason;

    protected JobExecutionException(JobFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public JobFailureReason getReason() {
        return reason;
    }
}
