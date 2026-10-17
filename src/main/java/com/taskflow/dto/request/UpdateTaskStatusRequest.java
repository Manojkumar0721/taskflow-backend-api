package com.taskflow.dto.request;

import com.taskflow.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating task status.
 */
public class UpdateTaskStatusRequest {

    @NotNull(message = "Task status is required (TODO, IN_PROGRESS, COMPLETED)")
    private TaskStatus status;

    public UpdateTaskStatusRequest() {
    }

    public UpdateTaskStatusRequest(TaskStatus status) {
        this.status = status;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
