package com.taskflow.service;

import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.request.UpdateProfileRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.dto.response.UserResponse;
import com.taskflow.entity.User;
import com.taskflow.enums.Role;
import com.taskflow.exception.DuplicateResourceException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.mapper.UserMapper;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "john@example.com", "encodedHash", "John", "Doe", Role.ROLE_USER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("register should encode password, save user, and return JWT with user details")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("john@example.com", "Password123", "John", "Doe");
        User unencodedUser = new User();
        unencodedUser.setEmail("john@example.com");

        UserResponse userResponse = new UserResponse(1L, "john@example.com", "John", "Doe", "ROLE_USER", LocalDateTime.now());

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(unencodedUser);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(userMapper.toResponse(sampleUser)).thenReturn(userResponse);

        AuthResponse authResponse = userService.register(request);

        assertThat(authResponse.getToken()).isEqualTo("sample.jwt.token");
        assertThat(authResponse.getUser().getEmail()).isEqualTo("john@example.com");
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(unencodedUser);
    }

    @Test
    @DisplayName("register should throw DuplicateResourceException when email already exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("john@example.com", "Password123", "John", "Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("login should authenticate credentials and return JWT token")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("john@example.com", "Password123");
        UserResponse userResponse = new UserResponse(1L, "john@example.com", "John", "Doe", "ROLE_USER", LocalDateTime.now());

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(userMapper.toResponse(sampleUser)).thenReturn(userResponse);

        AuthResponse authResponse = userService.login(request);

        assertThat(authResponse.getToken()).isEqualTo("sample.jwt.token");
        assertThat(authResponse.getUser().getEmail()).isEqualTo("john@example.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("getCurrentUserProfile should return current user from SecurityContext")
    void testGetCurrentUserProfile() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        UserResponse userResponse = new UserResponse(1L, "john@example.com", "John", "Doe", "ROLE_USER", LocalDateTime.now());
        when(userMapper.toResponse(sampleUser)).thenReturn(userResponse);

        UserResponse result = userService.getCurrentUserProfile();

        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("updateCurrentUserProfile should update first and last name")
    void testUpdateCurrentUserProfile() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse updatedResponse = new UserResponse(1L, "john@example.com", "Johnny", "Smith", "ROLE_USER", LocalDateTime.now());
        when(userMapper.toResponse(sampleUser)).thenReturn(updatedResponse);

        UpdateProfileRequest updateRequest = new UpdateProfileRequest("Johnny", "Smith");
        UserResponse result = userService.updateCurrentUserProfile(updateRequest);

        assertThat(result.getFirstName()).isEqualTo("Johnny");
        assertThat(result.getLastName()).isEqualTo("Smith");
        verify(userRepository).save(sampleUser);
    }
}
