package com.jobpulse.exception;

public class UsernameNotFoundException extends RuntimeException {
    public UsernameNotFoundException(String message) {
        super(message);
    }
    public UsernameNotFoundException(String message,Throwable tr) {
        super(message, tr);
    }
}
