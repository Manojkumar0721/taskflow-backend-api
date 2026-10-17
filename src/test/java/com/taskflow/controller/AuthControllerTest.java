package com.taskflow.controller;

import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.dto.response.UserResponse;
import com.taskflow.exception.DuplicateResourceException;
import com.taskflow.exception.GlobalExceptionHandler;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.security.JwtService;
import com.taskflow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 Created and AuthResponse")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("test@taskflow.com", "Password123", "John", "Doe");
        UserResponse userResponse = new UserResponse(1L, "test@taskflow.com", "John", "Doe", "ROLE_USER", LocalDateTime.now());
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", userResponse);

        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.user.email").value("test@taskflow.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email should return 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("duplicate@taskflow.com", "Password123", "John", "Doe");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("An account with email duplicate@taskflow.com already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK and token")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("test@taskflow.com", "Password123");
        UserResponse userResponse = new UserResponse(1L, "test@taskflow.com", "John", "Doe", "ROLE_USER", LocalDateTime.now());
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", userResponse);

        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.user.email").value("test@taskflow.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with bad credentials should return 401 Unauthorized")
    void testLoginBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@taskflow.com", "WrongPassword");

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
