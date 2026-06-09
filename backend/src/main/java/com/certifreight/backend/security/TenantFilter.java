package com.certifreight.backend.security;

import com.certifreight.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwtToken = authHeader.substring(7);

            try {
                Claims claims = jwtService.extractAllClaims(jwtToken);

                String tenantId = claims.get("tenantId", String.class);
                String userRole = claims.get("role", String.class); // Extract the single role claim (e.g. "ROLE_ADMIN")

                if (tenantId != null && userRole != null) {
                    TenantContext.setTenantId(tenantId);

                    // Convert the plain string role claim into an official Spring Security GrantedAuthority object
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(userRole));

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            tenantId, // Principal
                            null,     // Credentials
                            authorities // <-- CRITICAL: Passing authorities empowers @PreAuthorize to perform checks!
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception e) {
                log.error("Cryptographic token authentication failure: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Security Violation: Invalid or Expired Token");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}