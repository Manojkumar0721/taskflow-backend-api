package com.taskflow.mapper;

import com.taskflow.dto.request.CreateTaskRequest;
import com.taskflow.dto.response.TaskResponse;
import com.taskflow.entity.Task;
import org.springframework.stereotype.Component;

/**
 * Mapper component converting between Task JPA Entities and Task DTOs.
 */
@Component
public class TaskMapper {

    /**
     * Map CreateTaskRequest DTO to Task Entity.
     */
    public Task toEntity(CreateTaskRequest request) {
        if (request == null) {
            return null;
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        return task;
    }

    /**
     * Map Task Entity to TaskResponse DTO.
     */
    public TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
