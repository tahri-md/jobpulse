package com.jobpulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {
  @NotBlank(message = "Token is required")
  private String token;

  @NotBlank(message = "Password is required")
  @Size(min = 12, message = "Password must be at least 12 characters")
  private String newPassword;

  @NotBlank(message = "Password confirmation is required")
  private String passwordConfirmation;
}
