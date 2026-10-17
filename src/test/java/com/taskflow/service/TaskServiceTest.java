package com.taskflow.service;

import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.Role;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User(1L, "user@taskflow.com", "hash", "John", "Doe", Role.ROLE_USER);
    }

    @Test
    @DisplayName("createTask should attach current authenticated user and persist")
    void testCreateTask() {
        Task input = new Task();
        input.setTitle("Test Task");

        Task saved = new Task();
        saved.setId(1L);
        saved.setTitle("Test Task");
        saved.setUser(mockUser);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        Task result = taskService.createTask(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUser()).isEqualTo(mockUser);
        verify(taskRepository).save(input);
    }

    @Test
    @DisplayName("getTaskById should return task when owned by current user")
    void testGetTaskByIdFound() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setUser(mockUser);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTaskById should throw ResourceNotFoundException when task belongs to another user")
    void testGetTaskByIdNotOwned() {
        when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findByIdAndUser(999L, mockUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");
    }

    @Test
    @DisplayName("getTasks should return paginated tasks owned by current user")
    void testGetTasksPaginated() {
        Task t1 = new Task();
        t1.setId(1L);
        Page<Task> page = new PageImpl<>(List.of(t1));

        when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findByUserWithFilters(eq(mockUser), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<Task> result = taskService.getTasks(null, null, null, 0, 10, "createdAt", "desc");
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("updateTaskStatus should change status and save")
    void testUpdateTaskStatus() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setStatus(TaskStatus.TODO);
        existing.setUser(mockUser);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.updateTaskStatus(1L, TaskStatus.COMPLETED);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    @DisplayName("deleteTask should delete when task is owned by current user")
    void testDeleteTask() {
        Task existing = new Task();
        existing.setId(1L);
        existing.setUser(mockUser);

        when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
        when(taskRepository.findByIdAndUser(1L, mockUser)).thenReturn(Optional.of(existing));

        taskService.deleteTask(1L);

        verify(taskRepository).delete(existing);
    }
}
