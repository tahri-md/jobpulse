package com.jobpulse.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "key",
                "thisIsAStrongJwtSecretKeyForTestsAtLeastThirtyTwoChars");
    }

    @Test
    void generateAndExtractUsername() {
        String token = jwtService.generateToken("amine");

        assertNotNull(token);
        assertEquals("amine", jwtService.extractUsername(token));
    }
}
