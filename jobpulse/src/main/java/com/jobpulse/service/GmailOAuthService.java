package com.jobpulse.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.config.TokenEncryptionService;
import com.jobpulse.exception.BadRequestException;
import com.jobpulse.model.GmailToken;
import com.jobpulse.model.User;
import com.jobpulse.repository.GmailTokenRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Handles the Gmail OAuth2 flow:
 *   1. Build the Google consent URL (with offline access → gives us a refresh token)
 *   2. Exchange the authorization code for access + refresh tokens
 *   3. Store tokens encrypted in the database
 *   4. Refresh an expired access token transparently
 *   5. Provide a decrypted access token for the Gmail API
 */
@Service
@Slf4j
public class GmailOAuthService {

    @Value("${gmail.oauth.client-id}")
    private String clientId;

    @Value("${gmail.oauth.client-secret}")
    private String clientSecret;

    @Value("${gmail.oauth.redirect-uri}")
    private String redirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/userinfo.email";

    @Autowired
    private GmailTokenRepository gmailTokenRepository;

    @Autowired
    private TokenEncryptionService encryptionService;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    // ────────────────────── public API ──────────────────────

    /**
     * Returns the Google OAuth consent URL the frontend should redirect to.
     */
    public String buildAuthorizationUrl() {
        return GOOGLE_AUTH_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + GMAIL_SEND_SCOPE.replace(" ", "%20")
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=gmail_connect";
    }

    /**
     * Exchanges the authorization code for tokens and stores them.
     */
    @Transactional
    public GmailTokenInfo exchangeCodeAndStore(User user, String code) {
        log.info("Exchanging Gmail OAuth code for user: {}", user.getUsername());

        JsonNode tokenResponse = exchangeCode(code);
        return storeTokensFromResponse(user, tokenResponse);
    }

    /**
     * Stores Gmail tokens from an already-exchanged token response.
     * Use this when the code has already been exchanged elsewhere
     * (e.g., during Google OAuth login).
     */
    @Transactional
    public GmailTokenInfo storeTokensFromResponse(User user, JsonNode tokenResponse) {
        log.info("Storing Gmail tokens from pre-exchanged response for user: {}", user.getUsername());

        String accessToken  = tokenResponse.get("access_token").asText();
        String refreshToken = tokenResponse.has("refresh_token")
                ? tokenResponse.get("refresh_token").asText()
                : null;
        int expiresIn = tokenResponse.get("expires_in").asInt();

        if (refreshToken == null) {
            // If Google doesn't return a refresh token (user already granted access once)
            // try to keep the one we already have
            GmailToken existing = gmailTokenRepository.findByUser(user).orElse(null);
            if (existing != null) {
                refreshToken = encryptionService.decrypt(existing.getRefreshToken());
            } else {
                throw new BadRequestException(
                        "Google did not return a refresh token. Revoke access at "
                        + "https://myaccount.google.com/permissions and try again.");
            }
        }

        String gmailAddress = fetchGmailAddress(accessToken);

        GmailToken token = gmailTokenRepository.findByUser(user).orElse(
                GmailToken.builder().user(user).build());

        token.setAccessToken(encryptionService.encrypt(accessToken));
        token.setRefreshToken(encryptionService.encrypt(refreshToken));
        token.setGmailAddress(gmailAddress);
        token.setTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn - 60)); // 1 min buffer

        gmailTokenRepository.save(token);
        log.info("Gmail tokens stored for user: {} ({})", user.getUsername(), gmailAddress);

        return new GmailTokenInfo(gmailAddress, true);
    }

    /**
     * Returns a valid (refreshed if needed) decrypted access token for the given user.
     */
    @Transactional
    public String getValidAccessToken(User user) {
        GmailToken token = gmailTokenRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException(
                        "Gmail not connected. Connect your Gmail account first."));

        if (token.isExpired()) {
            log.info("Access token expired for user {}, refreshing...", user.getUsername());
            refreshAccessToken(token);
        }

        return encryptionService.decrypt(token.getAccessToken());
    }

    /**
     * Returns connection status for a user.
     */
    public GmailTokenInfo getConnectionStatus(User user) {
        return gmailTokenRepository.findByUser(user)
                .map(t -> new GmailTokenInfo(t.getGmailAddress(), true))
                .orElse(new GmailTokenInfo(null, false));
    }

    /**
     * Disconnects the user's Gmail account.
     */
    @Transactional
    public void disconnect(User user) {
        gmailTokenRepository.findByUser(user).ifPresent(token -> {
            // Best-effort revoke at Google
            try {
                String accessToken = encryptionService.decrypt(token.getAccessToken());
                webClient.post()
                        .uri("https://oauth2.googleapis.com/revoke?token=" + accessToken)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } catch (Exception e) {
                log.warn("Failed to revoke token at Google: {}", e.getMessage());
            }
            gmailTokenRepository.delete(token);
            log.info("Gmail disconnected for user: {}", user.getUsername());
        });
    }

    // ────────────────────── private helpers ──────────────────────

    private JsonNode exchangeCode(String code) {
        try {
            Map<String, String> body = Map.of(
                    "code", code,
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "redirect_uri", redirectUri,
                    "grant_type", "authorization_code");

            String response = webClient.post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);

            if (json.has("error")) {
                throw new BadRequestException("Google token exchange failed: " + json.get("error").asText());
            }

            return json;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token exchange failed: {}", e.getMessage());
            throw new BadRequestException("Failed to exchange Google authorization code: " + e.getMessage());
        }
    }

    private void refreshAccessToken(GmailToken token) {
        try {
            String refreshToken = encryptionService.decrypt(token.getRefreshToken());

            Map<String, String> body = Map.of(
                    "refresh_token", refreshToken,
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "grant_type", "refresh_token");

            String response = webClient.post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);

            if (json.has("error")) {
                log.error("Token refresh failed: {}", json.get("error").asText());
                throw new BadRequestException(
                        "Gmail token refresh failed. Please reconnect your Gmail account.");
            }

            String newAccessToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt();

            token.setAccessToken(encryptionService.encrypt(newAccessToken));
            token.setTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn - 60));

            // Google may issue a new refresh token
            if (json.has("refresh_token")) {
                token.setRefreshToken(encryptionService.encrypt(json.get("refresh_token").asText()));
            }

            gmailTokenRepository.save(token);
            log.info("Access token refreshed for user: {}", token.getUser().getUsername());

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            throw new BadRequestException("Failed to refresh Gmail token: " + e.getMessage());
        }
    }

    private String fetchGmailAddress(String accessToken) {
        try {
            String response = webClient.get()
                    .uri(GOOGLE_USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            return json.get("email").asText();
        } catch (Exception e) {
            log.warn("Could not fetch Gmail address: {}", e.getMessage());
            return "unknown@gmail.com";
        }
    }

    // ────────────────────── DTOs ──────────────────────

    public record GmailTokenInfo(String gmailAddress, boolean connected) {}
}
