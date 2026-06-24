package com.jobpulse.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HttpJobPayload {
  private String url;
  private String method;
  private Map<String, String> headers;
  private Object body;
  private Integer timeoutSeconds;
}
