package com.certifreight.backend.controller;

import com.certifreight.backend.config.SecurityConfig;
import com.certifreight.backend.security.TenantFilter;
import com.certifreight.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TenantFilter tenantFilter;

    @BeforeEach
    void allowFilterChainInSliceTests() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(tenantFilter).doFilter(any(), any(), any());
    }

    @Test
    void shouldReturnTokenForRequestedTenantAndRole() throws Exception {
        when(jwtService.generateToken("alpha", "ROLE_ADMIN")).thenReturn("jwt-admin-alpha");

        mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "alpha")
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-admin-alpha"));

        verify(jwtService).generateToken("alpha", "ROLE_ADMIN");
    }

    @Test
    void shouldFallbackToDispatcherRoleWhenRoleNotProvided() throws Exception {
        when(jwtService.generateToken("enterprise-alpha", "ROLE_DISPATCHER")).thenReturn("jwt-default-role");

        mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "enterprise-alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-default-role"));

        verify(jwtService).generateToken("enterprise-alpha", "ROLE_DISPATCHER");
    }
}

