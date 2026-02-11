package com.jobpulse.exception;

import com.jobpulse.dto.others.JobFailureReason;


public class NonRetryableJobException extends JobExecutionException {
    
    public NonRetryableJobException(JobFailureReason reason, String message) {
        super(reason, message);
    }

    public NonRetryableJobException(JobFailureReason reason, String message, Throwable cause) {
        super(reason, message);
        initCause(cause);
    }
}
