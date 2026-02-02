package com.jobpulse.service;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.dto.response.OAuthUserInfo;
import com.jobpulse.exception.BadRequestException;
import com.jobpulse.model.AuthProvider;
import com.jobpulse.model.Role;
import com.jobpulse.model.User;
import com.jobpulse.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.github.client-id}")
    private String githubClientId;

    @Value("${oauth.github.client-secret}")
    private String githubClientSecret;

    /**
     * Process Google OAuth token and get or create user
     */
    public User processGoogleOAuth(String idToken) {
        log.info("Processing Google OAuth");
        OAuthUserInfo userInfo = verifyGoogleToken(idToken);
        return getOrCreateUser(userInfo, AuthProvider.GOOGLE);
    }

    /**
     * Process GitHub OAuth code and get or create user
     */
    public User processGitHubOAuth(String code) {
        log.info("Processing GitHub OAuth with code: {}", code.substring(0, Math.min(10, code.length())) + "...");
        String accessToken = exchangeGitHubCodeForToken(code);
        OAuthUserInfo userInfo = getGitHubUserInfo(accessToken);
        return getOrCreateUser(userInfo, AuthProvider.GITHUB);
    }

   
    private OAuthUserInfo verifyGoogleToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new BadRequestException("Invalid JWT format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payloadNode = objectMapper.readTree(payload);

            if (!payloadNode.has("email")) {
                throw new BadRequestException("Invalid Google token: missing email");
            }

            String email = payloadNode.get("email").asText();
            String name = payloadNode.has("name") ? payloadNode.get("name").asText() : email.split("@")[0];
            String picture = payloadNode.has("picture") ? payloadNode.get("picture").asText() : null;
            String sub = payloadNode.get("sub").asText();

            log.debug("Extracted Google user: {} ({})", email, sub);

            return OAuthUserInfo.builder()
                    .email(email)
                    .name(name)
                    .avatar(picture)
                    .provider("GOOGLE")
                    .providerId(sub)
                    .build();

        } catch (Exception e) {
            log.error("Failed to verify Google token: {}", e.getMessage());
            throw new BadRequestException("Invalid Google token: " + e.getMessage());
        }
    }


    private String exchangeGitHubCodeForToken(String code) {
        try {
            Map<String, String> requestBody = Map.of(
                    "client_id", githubClientId,
                    "client_secret", githubClientSecret,
                    "code", code);

            String response = webClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);

            if (jsonResponse.has("error")) {
                String error = jsonResponse.get("error").asText();
                log.error("GitHub OAuth error: {}", error);
                throw new BadRequestException("GitHub authentication failed: " + error);
            }

            String accessToken = jsonResponse.get("access_token").asText();
            log.debug("Successfully exchanged GitHub code for access token");
            return accessToken;

        } catch (Exception e) {
            log.error("Failed to exchange GitHub code: {}", e.getMessage());
            throw new BadRequestException("Failed to authenticate with GitHub: " + e.getMessage());
        }
    }

    private OAuthUserInfo getGitHubUserInfo(String accessToken) {
        try {
            String response = webClient.get()
                    .uri("https://api.github.com/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode userNode = objectMapper.readTree(response);

            if (!userNode.has("id")) {
                throw new BadRequestException("Invalid GitHub response: missing id");
            }

            String login = userNode.get("login").asText();
            String name = userNode.has("name") && !userNode.get("name").isNull()
                    ? userNode.get("name").asText()
                    : login;
            String email = userNode.has("email") && !userNode.get("email").isNull()
                    ? userNode.get("email").asText()
                    : login + "@github.com";
            String avatar = userNode.has("avatar_url") ? userNode.get("avatar_url").asText() : null;
            String id = userNode.get("id").asText();

            log.debug("Extracted GitHub user: {} ({})", email, id);

            return OAuthUserInfo.builder()
                    .email(email)
                    .name(name)
                    .avatar(avatar)
                    .provider("GITHUB")
                    .providerId(id)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get GitHub user info: {}", e.getMessage());
            throw new BadRequestException("Failed to retrieve GitHub user info: " + e.getMessage());
        }
    }

   
    private User getOrCreateUser(OAuthUserInfo userInfo, AuthProvider provider) {
        User user = userRepository.findAll().stream()
                .filter(u -> userInfo.getProviderId().equals(u.getProviderId()) &&
                        u.getProvider() == provider)
                .findFirst()
                .orElse(null);

        if (user != null) {
            log.info("Found existing user: {} from provider: {}", user.getEmail(), provider);
            if (userInfo.getAvatar() != null) {
                user.setAvatar(userInfo.getAvatar());
                userRepository.save(user);
            }
            return user;
        }

        user = userRepository.findByEmail(userInfo.getEmail()).orElse(null);

        if (user != null) {
            log.info("Linking OAuth provider {} to existing user: {}", provider, user.getEmail());
            user.setProvider(provider);
            user.setProviderId(userInfo.getProviderId());
            if (userInfo.getAvatar() != null) {
                user.setAvatar(userInfo.getAvatar());
            }
            return userRepository.save(user);
        }

        log.info("Creating new user from {} OAuth: {}", provider, userInfo.getEmail());
        user = User.builder()
                .email(userInfo.getEmail())
                .username(generateUsername(userInfo.getEmail()))
                .password("") 
                .avatar(userInfo.getAvatar())
                .role(Role.USER)
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .build();

        return userRepository.save(user);
    }

   
    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
