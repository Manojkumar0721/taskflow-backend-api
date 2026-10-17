package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Task entity with user ownership enforcement.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Find a task by its ID and owner user.
     * Guarantees a user cannot read, update, or delete another user's task.
     */
    Optional<Task> findByIdAndUser(Long id, User user);

    /**
     * Dynamic multi-criteria search and filter with pagination, restricted to tasks owned by user.
     */
    @Query("SELECT t FROM Task t WHERE " +
           "t.user = :user AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> findByUserWithFilters(
            @Param("user") User user,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Retrieve paginated overdue tasks for a specific user.
     */
    Page<Task> findByUserAndDueDateBeforeAndStatusNot(User user, LocalDate date, TaskStatus status, Pageable pageable);

    /**
     * Retrieve paginated tasks due within a date range for a specific user.
     */
    Page<Task> findByUserAndDueDateBetween(User user, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // List queries for repository unit testing
    List<Task> findByStatus(TaskStatus status);

    List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

    List<Task> findByTitleContainingIgnoreCase(String keyword);
}
