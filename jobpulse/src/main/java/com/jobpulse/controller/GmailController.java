package com.jobpulse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jobpulse.model.User;
import com.jobpulse.service.GmailOAuthService;
import com.jobpulse.service.GmailOAuthService.GmailTokenInfo;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gmail")
@Slf4j
public class GmailController {

    @Autowired
    private GmailOAuthService gmailOAuthService;

    /**
     * Returns the Google OAuth consent URL.
     * The frontend opens this URL in a popup / redirect.
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String url = gmailOAuthService.buildAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Called after the user authorises in Google's consent screen.
     * The frontend sends the authorization code it received from the redirect.
     */
    @PostMapping("/callback")
    public ResponseEntity<GmailTokenInfo> handleCallback(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Gmail OAuth callback for user: {}", user.getUsername());
        GmailTokenInfo info = gmailOAuthService.exchangeCodeAndStore(user, code);
        return ResponseEntity.ok(info);
    }

    /**
     * Returns the connection status for the authenticated user.
     */
    @GetMapping("/status")
    public ResponseEntity<GmailTokenInfo> getStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gmailOAuthService.getConnectionStatus(user));
    }

    /**
     * Disconnects the user's Gmail account and revokes tokens.
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal User user) {
        gmailOAuthService.disconnect(user);
        return ResponseEntity.noContent().build();
    }
}
