package com.jobpulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 100, message = "Username should be between 3 and 100")
  private String username;

  @NotBlank(message = "Email is required")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 12, message = "Password must be at least 12 characters")
  private String password;

  @NotBlank(message = "Password confirmation is required")
  private String passwordConfirmation;
}
