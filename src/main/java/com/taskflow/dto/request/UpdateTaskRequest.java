package com.taskflow.dto.request;

import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO for full update of an existing task.
 */
public class UpdateTaskRequest {

    @NotBlank(message = "Task title is required and cannot be blank")
    @Size(min = 3, max = 150, message = "Task title must be between 3 and 150 characters")
    private String title;

    @Size(max = 2000, message = "Task description cannot exceed 2000 characters")
    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    @FutureOrPresent(message = "Due date must be today or in the future")
    private LocalDate dueDate;

    public UpdateTaskRequest() {
    }

    public UpdateTaskRequest(String title, String description, TaskStatus status,
                             TaskPriority priority, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
