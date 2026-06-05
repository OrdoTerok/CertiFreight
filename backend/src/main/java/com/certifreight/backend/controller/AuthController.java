package com.certifreight.backend.controller;

import com.certifreight.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService jwtService;

    /**
     * Authenticates an organization profile and issues a cryptographically signed JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestParam String tenantId) {
        log.info("Authentication requested for Tenant ID: '{}'", tenantId);

        if (tenantId == null || tenantId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String token = jwtService.generateToken(tenantId);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    /**
     * A lightweight, immutable Java 21 record to cleanly structure the outbound JSON payload.
     */
    public record AuthResponse(String accessToken) {}
}