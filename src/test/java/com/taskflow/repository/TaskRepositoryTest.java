package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer integration tests using @DataJpaTest.
 * Tests entity mapping, lifecycle hooks (@PrePersist), and custom derived queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("save should persist entity and populate @PrePersist timestamps and defaults")
    void testSaveTask() {
        Task task = new Task();
        task.setTitle("Test JPA Persistence");
        task.setDescription("Verify Hibernate auto timestamps");
        task.setPriority(TaskPriority.HIGH);

        Task saved = taskRepository.save(task);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByStatus should return only tasks with matching status")
    void testFindByStatus() {
        Task t1 = new Task();
        t1.setTitle("Task 1");
        t1.setStatus(TaskStatus.TODO);
        taskRepository.save(t1);

        Task t2 = new Task();
        t2.setTitle("Task 2");
        t2.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(t2);

        List<Task> todoTasks = taskRepository.findByStatus(TaskStatus.TODO);
        assertThat(todoTasks).hasSize(1);
        assertThat(todoTasks.get(0).getTitle()).isEqualTo("Task 1");
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase should perform case-insensitive keyword search")
    void testFindByTitleSearch() {
        Task task = new Task();
        task.setTitle("Spring Boot Deep Dive");
        taskRepository.save(task);

        List<Task> results = taskRepository.findByTitleContainingIgnoreCase("boot");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Spring Boot Deep Dive");
    }

    @Test
    @DisplayName("findByDueDateBeforeAndStatusNot should retrieve overdue tasks")
    void testOverdueTasks() {
        Task overdue = new Task();
        overdue.setTitle("Past Task");
        overdue.setDueDate(LocalDate.now().minusDays(2));
        overdue.setStatus(TaskStatus.TODO);
        taskRepository.save(overdue);

        Task completedPast = new Task();
        completedPast.setTitle("Completed Past");
        completedPast.setDueDate(LocalDate.now().minusDays(2));
        completedPast.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(completedPast);

        List<Task> overdueTasks = taskRepository.findByDueDateBeforeAndStatusNot(LocalDate.now(), TaskStatus.COMPLETED);
        assertThat(overdueTasks).hasSize(1);
        assertThat(overdueTasks.get(0).getTitle()).isEqualTo("Past Task");
    }
}
