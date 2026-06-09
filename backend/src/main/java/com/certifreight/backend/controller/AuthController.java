package com.certifreight.backend.controller;

import com.certifreight.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "ROLE_DISPATCHER") String role) {

        String jwtToken = jwtService.generateToken(tenantId, role);

        return ResponseEntity.ok(Map.of("accessToken", jwtToken));
    }
}