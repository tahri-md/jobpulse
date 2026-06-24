package com.jobpulse.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
  private boolean success;
  private String message;
  private T data;
  private long timestamp;

  public static <T> ApiResponse<T> ok(String message, T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .timestamp(System.currentTimeMillis())
        .build();
  }

  public static <T> ApiResponse<T> ok(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message("Operation successful")
        .data(data)
        .timestamp(System.currentTimeMillis())
        .build();
  }

  public static <T> ApiResponse<T> error(String message) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .timestamp(System.currentTimeMillis())
        .build();
  }

  public static <T> ApiResponse<T> error(String message, T data) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .data(data)
        .timestamp(System.currentTimeMillis())
        .build();
  }
}
