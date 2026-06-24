package com.jobpulse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordValidationService {

  @Value("${auth.password.min-length:12}")
  private int minLength;

  @Value("${auth.password.require-special-chars:true}")
  private boolean requireSpecialChars;

  @Value("${auth.password.require-numbers:true}")
  private boolean requireNumbers;

  @Value("${auth.password.require-uppercase:true}")
  private boolean requireUppercase;

  public String validatePassword(String password) {
    if (password == null || password.isEmpty()) {
      return "Password is required";
    }

    if (password.length() < minLength) {
      return String.format("Password must be at least %d characters long", minLength);
    }

    if (requireUppercase && !password.matches(".*[A-Z].*")) {
      return "Password must contain at least one uppercase letter";
    }

    if (requireNumbers && !password.matches(".*[0-9].*")) {
      return "Password must contain at least one number";
    }

    if (requireSpecialChars
        && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*")) {
      return "Password must contain at least one special character (!@#$%^&*()_+-=[]{};:'\",.<>?/\\|`~)";
    }

    return null; // Valid
  }

  public boolean isPasswordStrong(String password) {
    return validatePassword(password) == null;
  }
}
