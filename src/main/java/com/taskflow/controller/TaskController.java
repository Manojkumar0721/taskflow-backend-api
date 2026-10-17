package com.taskflow.controller;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.request.UpdateTaskRequest;
import com.taskflow.dto.request.UpdateTaskStatusRequest;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.Task;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * TaskController exposing RESTful APIs with advanced filtering, sorting, and pagination.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Endpoints for creating, retrieving, updating, filtering, and deleting user tasks")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    /**
     * POST /api/v1/tasks
     * Create a new task.
     */
    @Operation(summary = "Create a task", description = "Creates a new task associated with the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Task taskEntity = taskMapper.toEntity(request);
        Task created = taskService.createTask(taskEntity);
        return new ResponseEntity<>(taskMapper.toResponse(created), HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/tasks
     * Retrieve paginated, sorted, and filtered tasks.
     * Example: GET /api/v1/tasks?status=TODO&priority=HIGH&page=0&size=10&sortBy=createdAt&direction=desc
     */
    @Operation(summary = "List and filter tasks", description = "Retrieves a paginated list of tasks owned by the authenticated user with optional filtering by status, priority, and keyword search.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> getTasks(
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "priority", required = false) TaskPriority priority,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "desc") String direction) {

        Page<Task> taskPage = taskService.getTasks(status, priority, search, page, size, sortBy, direction);
        Page<TaskResponse> responsePage = taskPage.map(taskMapper::toResponse);
        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    /**
     * GET /api/v1/tasks/{id}
     * Retrieve a task by ID.
     */
    @Operation(summary = "Get task by ID", description = "Retrieves a specific task by ID. Returns 404 if the task does not exist or belongs to another user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found and returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Task not found or not owned by authenticated user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable("id") Long id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    /**
     * PUT /api/v1/tasks/{id}
     * Update an entire task.
     */
    @Operation(summary = "Update task", description = "Updates all fields of an owned task by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error on payload"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Task not found or not owned by authenticated user")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Task taskDetails = new Task();
        taskDetails.setTitle(request.getTitle());
        taskDetails.setDescription(request.getDescription());
        taskDetails.setStatus(request.getStatus());
        taskDetails.setPriority(request.getPriority());
        taskDetails.setDueDate(request.getDueDate());

        Task updated = taskService.updateTask(id, taskDetails);
        return ResponseEntity.ok(taskMapper.toResponse(updated));
    }

    /**
     * PATCH /api/v1/tasks/{id}/status
     * Update task status.
     */
    @Operation(summary = "Update task status", description = "Partially updates only the status (TODO, IN_PROGRESS, COMPLETED) of an owned task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        Task updated = taskService.updateTaskStatus(id, request.getStatus());
        return ResponseEntity.ok(taskMapper.toResponse(updated));
    }

    /**
     * DELETE /api/v1/tasks/{id}
     * Delete a task.
     */
    @Operation(summary = "Delete task", description = "Deletes an owned task by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/tasks/search?title=keyword
     * Convenience search endpoint with pagination.
     */
    @Operation(summary = "Search tasks by title", description = "Search user tasks containing the given title keyword (case-insensitive).")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<TaskResponse>> searchTasks(
            @RequestParam("title") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "desc") String direction) {

        Page<Task> taskPage = taskService.getTasks(null, null, keyword, page, size, sortBy, direction);
        Page<TaskResponse> responsePage = taskPage.map(taskMapper::toResponse);
        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    /**
     * GET /api/v1/tasks/overdue
     * Retrieve paginated overdue tasks.
     */
    @Operation(summary = "List overdue tasks", description = "Retrieves non-completed tasks whose due date is in the past.")
    @GetMapping("/overdue")
    public ResponseEntity<PageResponse<TaskResponse>> getOverdueTasks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "dueDate") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction) {

        Page<Task> taskPage = taskService.getOverdueTasks(page, size, sortBy, direction);
        Page<TaskResponse> responsePage = taskPage.map(taskMapper::toResponse);
        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    /**
     * GET /api/v1/tasks/due-between?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     * Retrieve paginated tasks due within a date range.
     */
    @Operation(summary = "List tasks due in date range", description = "Retrieves tasks due between the specified start and end dates inclusive.")
    @GetMapping("/due-between")
    public ResponseEntity<PageResponse<TaskResponse>> getTasksDueBetween(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "dueDate") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction) {

        Page<Task> taskPage = taskService.getTasksDueBetween(startDate, endDate, page, size, sortBy, direction);
        Page<TaskResponse> responsePage = taskPage.map(taskMapper::toResponse);
        return ResponseEntity.ok(PageResponse.from(responsePage));
    }
}
