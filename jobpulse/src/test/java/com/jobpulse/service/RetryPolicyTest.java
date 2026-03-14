package com.jobpulse.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jobpulse.dto.others.JobFailureReason;

class RetryPolicyTest {

    private final RetryPolicy retryPolicy = new RetryPolicy();

    @Test
    void retryableReasonsReturnTrue() {
        assertTrue(retryPolicy.isRetryable(JobFailureReason.NETWORK_ERROR));
        assertTrue(retryPolicy.isRetryable(JobFailureReason.TIMEOUT));
        assertTrue(retryPolicy.isRetryable(JobFailureReason.RATE_LIMITED));
        assertTrue(retryPolicy.isRetryable(JobFailureReason.REMOTE_5XX));
    }

    @Test
    void nonRetryableReasonsReturnFalse() {
        assertFalse(retryPolicy.isRetryable(JobFailureReason.BAD_REQUEST));
        assertFalse(retryPolicy.isRetryable(JobFailureReason.AUTH_ERROR));
        assertFalse(retryPolicy.isRetryable(JobFailureReason.INVALID_CONFIG));
        assertFalse(retryPolicy.isRetryable(JobFailureReason.UNKNOWN));
    }
}
