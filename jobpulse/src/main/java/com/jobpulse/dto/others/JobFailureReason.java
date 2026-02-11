package com.jobpulse.dto.others;

public enum JobFailureReason {
    NETWORK_ERROR,
    TIMEOUT,
    RATE_LIMITED,
    REMOTE_5XX,
    BAD_REQUEST,
    AUTH_ERROR,
    INVALID_CONFIG,
    UNKNOWN
}
