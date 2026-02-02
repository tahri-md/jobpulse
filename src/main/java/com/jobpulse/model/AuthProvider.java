package com.jobpulse.model;

public enum AuthProvider {
    NATIVE("Native"),
    GOOGLE("Google"),
    GITHUB("GitHub");

    public final String displayName;

    AuthProvider(String displayName) {
        this.displayName = displayName;
    }
}
