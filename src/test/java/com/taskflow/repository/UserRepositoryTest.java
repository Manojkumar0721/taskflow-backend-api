package com.taskflow.repository;

import com.taskflow.entity.User;
import com.taskflow.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByEmail should return user when email exists")
    void testFindByEmailExists() {
        User user = new User(null, "unique@taskflow.com", "hashedPassword", "John", "Doe", Role.ROLE_USER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("unique@taskflow.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByEmail should return empty Optional when email does not exist")
    void testFindByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@taskflow.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail should return true if email is present, false otherwise")
    void testExistsByEmail() {
        User user = new User(null, "exists@taskflow.com", "hashedPassword", "Jane", "Doe", Role.ROLE_USER);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exists@taskflow.com")).isTrue();
        assertThat(userRepository.existsByEmail("notthere@taskflow.com")).isFalse();
    }

    @Test
    @DisplayName("Saving duplicate email should throw DataIntegrityViolationException")
    void testDuplicateEmailThrowsException() {
        User user1 = new User(null, "duplicate@taskflow.com", "pass1", "First", "User", Role.ROLE_USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User(null, "duplicate@taskflow.com", "pass2", "Second", "User", Role.ROLE_USER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
