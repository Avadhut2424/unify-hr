package com.unifyhr.auth.service;

import com.unifyhr.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    void generateToken_And_ExtractDetails() {
        String token = jwtService.generateToken("alice", Role.ADMIN);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals(Role.ADMIN, jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"));
        assertFalse(jwtService.isTokenValid(""));
    }
}
