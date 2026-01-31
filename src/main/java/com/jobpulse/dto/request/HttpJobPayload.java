package com.jobpulse.dto.request;

import java.util.Map;

import lombok.Data;

@Data
public class HttpJobPayload {
    public String url;
    public String method;
    public Map<String, String> headers;
    public Object body;
    public Integer timeoutSeconds;
}

