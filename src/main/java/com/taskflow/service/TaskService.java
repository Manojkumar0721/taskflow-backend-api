package com.taskflow.service;

import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Service orchestrating Task business operations with strict User ownership enforcement.
 */
@Service
@Transactional
public class TaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "status", "priority", "dueDate", "createdAt", "updatedAt"
    );

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    /**
     * Retrieve paginated and filtered tasks owned exclusively by the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<Task> getTasks(TaskStatus status, TaskPriority priority, String search,
                               int page, int size, String sortBy, String direction) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Pageable pageable = createPageable(page, size, sortBy, direction);
        return taskRepository.findByUserWithFilters(currentUser, status, priority, search, pageable);
    }

    /**
     * Retrieve a task by ID ensuring it belongs to the authenticated user.
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return taskRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    /**
     * Create a new task tied to the currently authenticated user.
     */
    public Task createTask(Task task) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        task.setUser(currentUser);
        return taskRepository.save(task);
    }

    /**
     * Update an existing task owned by the authenticated user.
     */
    public Task updateTask(Long id, Task taskDetails) {
        Task existingTask = getTaskById(id); // Throws 404 if not found or belongs to another user

        existingTask.setTitle(taskDetails.getTitle());
        existingTask.setDescription(taskDetails.getDescription());
        if (taskDetails.getStatus() != null) {
            existingTask.setStatus(taskDetails.getStatus());
        }
        if (taskDetails.getPriority() != null) {
            existingTask.setPriority(taskDetails.getPriority());
        }
        existingTask.setDueDate(taskDetails.getDueDate());

        return taskRepository.save(existingTask);
    }

    /**
     * Partially update status for an owned task.
     */
    public Task updateTaskStatus(Long id, TaskStatus status) {
        Task existingTask = getTaskById(id);
        existingTask.setStatus(status);
        return taskRepository.save(existingTask);
    }

    /**
     * Delete an owned task.
     */
    public void deleteTask(Long id) {
        Task existingTask = getTaskById(id);
        taskRepository.delete(existingTask);
    }

    /**
     * Retrieve paginated overdue tasks for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<Task> getOverdueTasks(int page, int size, String sortBy, String direction) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Pageable pageable = createPageable(page, size, sortBy, direction);
        return taskRepository.findByUserAndDueDateBeforeAndStatusNot(currentUser, LocalDate.now(), TaskStatus.COMPLETED, pageable);
    }

    /**
     * Retrieve paginated tasks due within a date range for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<Task> getTasksDueBetween(LocalDate startDate, LocalDate endDate,
                                         int page, int size, String sortBy, String direction) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        User currentUser = userService.getCurrentAuthenticatedUser();
        Pageable pageable = createPageable(page, size, sortBy, direction);
        return taskRepository.findByUserAndDueDateBetween(currentUser, startDate, endDate, pageable);
    }

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(Math.max(1, size), 100);
        String sortProperty = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageNumber, pageSize, Sort.by(sortDirection, sortProperty));
    }
}
