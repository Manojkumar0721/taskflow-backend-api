package com.taskflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.enums.TaskPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Protected endpoint without JWT should return 401 Unauthorized")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Public auth endpoints should be accessible without token")
    void testPublicAuthEndpoints() throws Exception {
        RegisterRequest register = new RegisterRequest("newbie@example.com", "Password123!", "New", "User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value("newbie@example.com"));
    }

    @Test
    @DisplayName("User can access protected tasks endpoint with valid JWT token")
    void testProtectedEndpointWithValidJwt() throws Exception {
        // 1. Register a user
        RegisterRequest register = new RegisterRequest("jwtuser@example.com", "Password123!", "JWT", "User");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        String token = authResponse.getToken();

        // 2. Access /api/v1/tasks using Bearer token
        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("User isolation: User B cannot access or view User A's tasks")
    void testUserTaskIsolation() throws Exception {
        // 1. Register User A and create a task
        RegisterRequest userARequest = new RegisterRequest("usera@example.com", "Password123!", "Alice", "User");
        MvcResult userAResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userARequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = objectMapper.readValue(userAResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        CreateTaskRequest taskRequest = new CreateTaskRequest();
        taskRequest.setTitle("Alice's Secret Task");
        taskRequest.setDescription("Private notes");
        taskRequest.setPriority(TaskPriority.HIGH);
        taskRequest.setDueDate(LocalDate.now().plusDays(5));

        MvcResult taskResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        TaskResponse createdTask = objectMapper.readValue(taskResult.getResponse().getContentAsString(), TaskResponse.class);
        Long taskId = createdTask.getId();

        // 2. Register User B
        RegisterRequest userBRequest = new RegisterRequest("userb@example.com", "Password123!", "Bob", "User");
        MvcResult userBResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenB = objectMapper.readValue(userBResult.getResponse().getContentAsString(), AuthResponse.class).getToken();

        // 3. User B retrieves tasks -> list must be empty (cannot see User A's task)
        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        // 4. User B attempts to access User A's task by ID -> returns 404 Not Found
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
