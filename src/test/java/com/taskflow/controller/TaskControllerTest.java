package com.taskflow.controller;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskStatusRequest;
import com.taskflow.entity.Task;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.GlobalExceptionHandler;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.security.JwtService;
import com.taskflow.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filter chain for isolated controller unit test
@Import({TaskMapper.class, GlobalExceptionHandler.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/tasks should return 201 Created when request is valid")
    void testCreateTaskValid() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Task Title");
        request.setDescription("Valid Description");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(2));

        Task saved = new Task(1L, "Valid Task Title", "Valid Description", TaskStatus.TODO,
                TaskPriority.HIGH, LocalDate.now().plusDays(2), LocalDateTime.now(), LocalDateTime.now());

        when(taskService.createTask(any(Task.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Valid Task Title"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks should return 400 Bad Request when title is blank")
    void testCreateTaskBlankTitleValidation() throws Exception {
        CreateTaskRequest invalid = new CreateTaskRequest();
        invalid.setTitle("");

        mockMvc.perform(post("/api/v1/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @DisplayName("GET /api/v1/tasks should return 200 OK with paginated PageResponse")
    void testGetTasksPaginated() throws Exception {
        Task task1 = new Task(1L, "Task 1", "Desc 1", TaskStatus.TODO, TaskPriority.LOW, null, null, null);
        PageImpl<Task> page = new PageImpl<>(List.of(task1));

        when(taskService.getTasks(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks?page=0&size=10&sortBy=createdAt&direction=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} should return 404 Not Found when task does not exist")
    void testGetTaskNotFound() throws Exception {
        when(taskService.getTaskById(999L)).thenThrow(new ResourceNotFoundException("Task not found with id: 999"));

        mockMvc.perform(get("/api/v1/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id: 999"));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status should update task status with JSON body")
    void testUpdateTaskStatus() throws Exception {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);
        Task updated = new Task(1L, "Task 1", "Desc", TaskStatus.COMPLETED, TaskPriority.LOW, null, null, null);

        when(taskService.updateTaskStatus(1L, TaskStatus.COMPLETED)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/tasks/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} should return 204 No Content")
    void testDeleteTask() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/v1/tasks/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/tasks/search should return paginated search results")
    void testSearchTasks() throws Exception {
        Task task = new Task(1L, "Spring Boot Guide", "Learn Spring", TaskStatus.TODO, TaskPriority.HIGH, null, null, null);
        PageImpl<Task> page = new PageImpl<>(List.of(task));

        when(taskService.getTasks(isNull(), isNull(), anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks/search?title=Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Spring Boot Guide"));
    }
}
