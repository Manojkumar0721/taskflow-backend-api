package com.taskflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.request.UpdateTaskStatusRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("integration@taskflow.com", "Password123!", "Integ", "User");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = auth.getToken();
    }

    @Test
    @DisplayName("End-to-End Task CRUD lifecycle")
    void testTaskCrudLifecycle() throws Exception {
        // 1. Create a task
        CreateTaskRequest create = new CreateTaskRequest();
        create.setTitle("End to End Task");
        create.setDescription("Integration testing CRUD");
        create.setPriority(TaskPriority.HIGH);
        create.setDueDate(LocalDate.now().plusDays(3));

        MvcResult createResult = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("End to End Task"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        TaskResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), TaskResponse.class);
        Long taskId = created.getId();

        // 2. Retrieve task by ID
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("End to End Task"));

        // 3. Update task
        UpdateTaskRequest update = new UpdateTaskRequest();
        update.setTitle("Updated Task Title");
        update.setDescription("Updated Description");
        update.setStatus(TaskStatus.IN_PROGRESS);
        update.setPriority(TaskPriority.MEDIUM);
        update.setDueDate(LocalDate.now().plusDays(5));

        mockMvc.perform(put("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task Title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        // 4. Update task status partially (PATCH)
        UpdateTaskStatusRequest patchStatus = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 5. Delete task
        mockMvc.perform(delete("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // 6. Verify task no longer exists
        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Autowired
    private com.taskflow.repository.TaskRepository taskRepository;

    @Autowired
    private com.taskflow.repository.UserRepository userRepository;

    @Test
    @DisplayName("Validation: POST /api/v1/tasks with past due date should return 400 Bad Request")
    void testPastDueDateValidation() throws Exception {
        CreateTaskRequest invalid = new CreateTaskRequest();
        invalid.setTitle("Past Due Task");
        invalid.setDueDate(LocalDate.now().minusDays(2));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.dueDate").exists());
    }

    @Test
    @DisplayName("Filter, search, pagination, and overdue integration checks")
    void testFilterAndSearchTasks() throws Exception {
        // 1. Seed a past due task directly in DB (simulating time passing)
        com.taskflow.entity.User user = userRepository.findByEmail("integration@taskflow.com").orElseThrow();
        com.taskflow.entity.Task overdue = new com.taskflow.entity.Task();
        overdue.setTitle("Overdue Report");
        overdue.setDescription("Submitted last week");
        overdue.setDueDate(LocalDate.now().minusDays(2));
        overdue.setStatus(TaskStatus.TODO);
        overdue.setPriority(TaskPriority.HIGH);
        overdue.setUser(user);
        taskRepository.save(overdue);

        // 2. Create a future task via REST API
        CreateTaskRequest futureTask = new CreateTaskRequest();
        futureTask.setTitle("Spring Boot Release");
        futureTask.setPriority(TaskPriority.LOW);
        futureTask.setDueDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureTask)))
                .andExpect(status().isCreated());

        // Check search
        mockMvc.perform(get("/api/v1/tasks/search?title=Report")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Overdue Report"));

        // Check overdue
        mockMvc.perform(get("/api/v1/tasks/overdue")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Overdue Report"));

        // Check due-between
        mockMvc.perform(get("/api/v1/tasks/due-between?startDate=" + LocalDate.now() + "&endDate=" + LocalDate.now().plusDays(15))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot Release"));
    }
}
