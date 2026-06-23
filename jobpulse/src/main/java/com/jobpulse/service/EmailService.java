package com.jobpulse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Async
    public void sendEmailVerification(String email, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Verify Your Jobpulse Account Email");

            String verificationUrl = frontendUrl + "/verify-email?token=" + token;
            String content = buildEmailVerificationContent(username, verificationUrl);

            message.setText(content);
            mailSender.send(message);

            log.info("Email verification sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email verification to {}: {}", email, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String email, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Jobpulse Password Reset Request");

            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            String content = buildPasswordResetContent(username, resetUrl);

            message.setText(content);
            mailSender.send(message);

            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangedNotification(String email, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Jobpulse Password Changed");

            String content = buildPasswordChangedContent(username);

            message.setText(content);
            mailSender.send(message);

            log.info("Password changed notification sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password changed notification to {}: {}", email, e.getMessage());
        }
    }

    @Async
    public void sendRoleAssignmentNotification(String email, String username, String role) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Jobpulse Account Permissions Updated");

            String content = buildRoleAssignmentContent(username, role);

            message.setText(content);
            mailSender.send(message);

            log.info("Role assignment notification sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send role assignment notification to {}: {}", email, e.getMessage());
        }
    }

    private String buildEmailVerificationContent(String username, String verificationUrl) {
        return String.format(
                "Hello %s,\n\n" +
                        "Welcome to Jobpulse Platform!\n\n" +
                        "Please verify your email address by clicking the link below:\n" +
                        "%s\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "If you did not create this account, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Jobpulse Team",
                username, verificationUrl
        );
    }

    private String buildPasswordResetContent(String username, String resetUrl) {
        return String.format(
                "Hello %s,\n\n" +
                        "We received a request to reset your Jobpulse password.\n\n" +
                        "Click the link below to reset your password:\n" +
                        "%s\n\n" +
                        "This link will expire in 1 hour.\n\n" +
                        "If you did not request this reset, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Jobpulse Team",
                username, resetUrl
        );
    }

    private String buildPasswordChangedContent(String username) {
        return String.format(
                "Hello %s,\n\n" +
                        "Your Jobpulse account password has been changed successfully.\n\n" +
                        "If you did not make this change, please reset your password immediately.\n\n" +
                        "Best regards,\n" +
                        "Jobpulse Team",
                username
        );
    }

    private String buildRoleAssignmentContent(String username, String role) {
        return String.format(
                "Hello %s,\n\n" +
                        "Your permissions in Jobpulse have been updated.\n\n" +
                        "Your new role: %s\n\n" +
                        "Best regards,\n" +
                        "Jobpulse Team",
                username, role
        );
    }
}