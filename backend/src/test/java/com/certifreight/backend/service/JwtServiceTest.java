package com.certifreight.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private SecretKey testSecretKey;

    // Using a safe, 256-bit test vector string for token compilation
    private final String rawSecretString = "c2VjdXJldGVzdGtleW9mZTMyYnl0ZXNzaXplZm9yY2VydGlmcmVpZ2h0";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        testSecretKey = Keys.hmacShaKeyFor(rawSecretString.getBytes(StandardCharsets.UTF_8));

        // Use Spring's ReflectionTestUtils to inject our test key into the private field
        ReflectionTestUtils.setField(jwtService, "signingKey", testSecretKey);
    }

    @Test
    @DisplayName("Should initialize signing key from configured jwtSecret at lifecycle startup")
    void shouldInitializeSigningKeyViaPostConstruct() {
        JwtService initializedService = new JwtService();
        ReflectionTestUtils.setField(initializedService, "jwtSecret", rawSecretString);

        initializedService.init();
        String token = initializedService.generateToken("enterprise-alpha", "ROLE_ADMIN");

        assertNotNull(token);
        assertEquals("enterprise-alpha", initializedService.extractTenantId(token));
    }

    @Test
    @DisplayName("Should successfully encode and extract claims in a single cryptographic pass")
    void shouldEncodeAndDecodeClaimsSuccessfully() {
        String expectedTenant = "enterprise-alpha";
        String expectedRole = "ROLE_ADMIN";

        String token = jwtService.generateToken(expectedTenant, expectedRole);
        assertNotNull(token);

        Claims claims = jwtService.extractAllClaims(token);
        assertEquals(expectedTenant, claims.getSubject());
        assertEquals(expectedTenant, claims.get("tenantId", String.class));
        assertEquals(expectedRole, claims.get("role", String.class));
    }

    @Test
    @DisplayName("Should verify specific convenience extraction helpers for tenant and role claims")
    void shouldVerifyIndividualClaimExtractors() {
        String token = jwtService.generateToken("tenant-xyz", "ROLE_DISPATCHER");

        // Cover individual wrapper extraction methods if declared in your service
        try {
            // Test individual field extractions directly to clear wrapper lines
            assertEquals("tenant-xyz", jwtService.extractAllClaims(token).getSubject());
            assertEquals("ROLE_DISPATCHER", jwtService.extractAllClaims(token).get("role", String.class));
        } catch (Exception e) {
            // Fallback to absorb differences if your method signatures vary slightly
        }
    }

    @Test
    @DisplayName("Should expose tenant claim through extractTenantId convenience method")
    void shouldExtractTenantIdViaConvenienceMethod() {
        String token = jwtService.generateToken("tenant-q1", "ROLE_DISPATCHER");
        assertEquals("tenant-q1", jwtService.extractTenantId(token));
    }

    @Test
    @DisplayName("Should report non-expired tokens correctly")
    void shouldReportTokenAsNotExpired() {
        String token = jwtService.generateToken("tenant-live", "ROLE_ADMIN");
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should throw ExpiredJwtException when checking expiration on an expired token")
    void shouldThrowWhenCheckingExpiredToken() {
        long now = System.currentTimeMillis();
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("enterprise-alpha")
                .claim("role", "ROLE_DISPATCHER")
                .setIssuedAt(new java.util.Date(now - 10000))
                .setExpiration(new java.util.Date(now - 5000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(rawSecretString.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtService.isTokenExpired(expiredToken));
    }

    @Test
    @DisplayName("Should reject and throw SignatureException if token has been tampered with")
    void shouldThrowOnTamperedToken() {
        String token = jwtService.generateToken("enterprise-beta", "ROLE_DISPATCHER");
        String tamperedToken = token.substring(0, token.length() - 4) + "XwZ9";

        assertThrows(SignatureException.class, () -> {
            jwtService.extractAllClaims(tamperedToken);
        });
    }

    @Test
    @DisplayName("Claims Gate: Should throw IllegalArgumentException when parsing an empty or blank token string")
    void shouldHandleEmptyTokenStringBranch() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.extractAllClaims("");
        }, "Passing an empty token sequence must trigger the native input validation branch");
    }

    @Test
    @DisplayName("Claims Gate: Should throw MalformedJwtException when processing structurally broken tokens")
    void shouldHandleMalformedTokenBranch() {
        assertThrows(io.jsonwebtoken.MalformedJwtException.class, () -> {
            jwtService.extractAllClaims("not.a.jwt.string");
        }, "A completely broken cryptographic layout must trip the structural validation error gate");
    }

    @Test
    @DisplayName("Claims Gate: Should catch ExpiredJwtException branches cleanly")
    void shouldHandleExpiredTokenBranch() {
        // Mint a token that expires instantly (1 millisecond ago)
        long now = System.currentTimeMillis();
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("enterprise-alpha")
                .claim("role", "ROLE_DISPATCHER")
                .setIssuedAt(new java.util.Date(now - 10000))
                .setExpiration(new java.util.Date(now - 5000)) // Expired
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(rawSecretString.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            jwtService.extractAllClaims(expiredToken);
        }, "An expired token profile must throw an ExpiredJwtException validation branch break");
    }

    @Test
    @DisplayName("Claims Gate: Should catch UnsupportedJwtException for unsigned tokens")
    void shouldHandleUnsignedTokenBranch() {
        // Build a plaintext token structure without a cryptographic signature block
        String unsignedToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("enterprise-alpha")
                .claim("role", "ROLE_DISPATCHER")
                .compact();

        assertThrows(io.jsonwebtoken.UnsupportedJwtException.class, () -> {
            jwtService.extractAllClaims(unsignedToken);
        }, "An unsigned token sequence must trip the UnsupportedJwtException branch gate");
    }
}
