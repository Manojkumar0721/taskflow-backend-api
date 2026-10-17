package com.taskflow.mapper;

import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.UserResponse;
import com.taskflow.entity.User;
import com.taskflow.enums.Role;
import org.springframework.stereotype.Component;

/**
 * Mapper component converting between User entities and User DTOs.
 */
@Component
public class UserMapper {

    /**
     * Map RegisterRequest to User entity (password is encoded in the service layer).
     */
    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setRole(Role.ROLE_USER);
        return user;
    }

    /**
     * Map User entity to UserResponse DTO.
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
