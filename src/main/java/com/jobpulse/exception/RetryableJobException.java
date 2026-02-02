package com.jobpulse.exception;

import com.jobpulse.dto.others.JobFailureReason;


public class RetryableJobException extends JobExecutionException {
    
    public RetryableJobException(JobFailureReason reason, String message) {
        super(reason, message);
    }

    public RetryableJobException(JobFailureReason reason, String message, Throwable cause) {
        super(reason, message);
        initCause(cause);
    }
}
