# TaskFlow — Java Backend Task Management REST API

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)
![Security](https://img.shields.io/badge/Spring%20Security-JWT-blue.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)
![Tests](https://img.shields.io/badge/Tests-42%20Passed-success.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)

**TaskFlow** is a complete, production-ready, beginner-friendly task management REST API built with **Java 21**, **Spring Boot 3.4.3**, **Spring Security 6**, **JJWT 0.12.6**, **Spring Data JPA**, **Hibernate 6**, and **MySQL 8.x**.

It implements strict **Layered Architecture** (Controller → Service → Repository → Database), provides full **stateless JWT authentication**, **BCrypt password hashing**, **Jakarta Bean Validation**, **Flyway database migrations**, and **multi-tenant task isolation** (users can exclusively access and manage their own tasks).

---

## Table of Contents
1. [Architecture & Design](#architecture--design)
2. [Technology Stack](#technology-stack)
3. [Database Schema & Migrations](#database-schema--migrations)
4. [REST API Endpoints](#rest-api-endpoints)
5. [Getting Started & Local Setup](#getting-started--local-setup)
6. [Interactive Documentation (Swagger UI)](#interactive-documentation-swagger-ui)
7. [Postman Collection](#postman-collection)
8. [Docker & Containerized Deployment](#docker--containerized-deployment)
9. [Automated Testing Suite](#automated-testing-suite)
10. [Annotations Master Reference](#annotations-master-reference)
11. [Interview Questions & Answers Guide](#interview-questions--answers-guide)
12. [Troubleshooting Guide](#troubleshooting-guide)
13. [Limitations & Production Readiness](#limitations--production-readiness)

---

## Architecture & Design

### Layered Architecture Workflow
```
[ HTTP Client / Postman / Swagger UI ]
                 │
                 ▼ (HTTP Request with "Authorization: Bearer <JWT>")
[ JwtAuthenticationFilter ] (OncePerRequestFilter)
  ├── Extracts Bearer token
  ├── Validates token signature & expiration using JwtService
  ├── Loads UserDetails from UserRepository
  └── Populates SecurityContextHolder with UsernamePasswordAuthenticationToken
                 │
                 ▼
[ SecurityFilterChain ]
  ├── Evaluates endpoint authorization rules
  └── Missing/Invalid Token ──▶ 401 Unauthorized (ApiErrorResponse)
                 │
                 ▼
[ Controller Layer ] (AuthController, UserController, TaskController)
  ├── Injects DTOs with @Valid bean validation
  ├── Rejects invalid inputs ──▶ 400 Bad Request via @RestControllerAdvice
  └── Invokes Service layer via constructor-based dependency injection
                 │
                 ▼
[ Service Layer ] (UserService, TaskService)
  ├── @Transactional management
  ├── Enforces business rules & ownership isolation
  └── Derives task owner directly from SecurityContext (prevents IDOR)
                 │
                 ▼
[ Mapper Layer ] (UserMapper, TaskMapper)
  └── Converts safely between Entities and DTOs (prevents entity/hash leakage)
                 │
                 ▼
[ Repository Layer ] (UserRepository, TaskRepository)
  ├── Spring Data JPA & JPQL queries
  └── Scopes every task query by authenticated user (findByIdAndUser)
                 │
                 ▼ (HikariCP Connection Pool)
[ MySQL 8.x Database / H2 Test DB ]
  └── Schema versioned and evolved with Flyway migrations
```

### Layer Responsibilities
* **Controller:** Handles HTTP request mapping, parameter binding, invokes validation, delegates to services, and returns HTTP response status codes. Contains **zero business logic**.
* **Service:** Encapsulates all business rules, coordinates transaction boundaries (`@Transactional`), hashes passwords, and isolates user resources.
* **Repository:** Manages database interaction using Spring Data JPA, derived query methods, and JPQL.
* **Entity:** Represents normalized database tables (`users`, `tasks`) mapped via JPA.
* **DTO (Data Transfer Object):** Strongly-typed request and response contracts isolating internal entity structure from external consumers.
* **Mapper:** Decouples entities from DTOs to eliminate circular references and hide sensitive data (like password hashes).
* **Exception Handler:** Global centralized interception using `@RestControllerAdvice` returning standard `ApiErrorResponse` envelopes.
* **Security:** Configures stateless authentication filter chains, CORS, CSRF disablement, BCrypt password encoders, and JWT signing/verification.

---

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 (LTS) | Core programming language (modern records, pattern matching, virtual threads ready) |
| **Spring Boot** | 3.4.3 | Application framework and autoconfiguration |
| **Spring Security** | 6.x | Authentication, authorization, and security filter chains |
| **JJWT (Java JWT)** | 0.12.6 | HMAC-SHA256 JWT generation, signing, and parsing |
| **Spring Data JPA** | 3.4.3 | Repository abstraction and Hibernate 6 ORM persistence |
| **MySQL Connector/J** | 8.x | JDBC driver for MySQL 8 relational database |
| **Flyway** | 10.x | Reproducible, version-controlled database migrations |
| **Jakarta Bean Validation** | 3.0 | Declarative input validation (`@NotBlank`, `@Size`, `@FutureOrPresent`) |
| **Springdoc OpenAPI** | 2.8.5 | Automated Swagger UI & OpenAPI 3.0 interactive API documentation |
| **H2 Database** | 2.3.x | In-memory database with MySQL compatibility mode for rapid testing |
| **JUnit 5 & Mockito** | Latest | Unit testing, slice testing, and full integration testing |
| **Docker & Compose** | Latest | Containerization for reproducible application and database deployment |

---

## Database Schema & Migrations

Database evolution is versioned through Flyway migration scripts in `src/main/resources/db/migration`:

### Migration `V1__init_schema.sql`
Creates the initial `tasks` table with indexes:
```sql
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_priority (priority),
    INDEX idx_tasks_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Migration `V2__create_users_table.sql`
Creates the `users` table and establishes the foreign key relationship with cascading deletes:
```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE tasks ADD COLUMN user_id BIGINT;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE tasks ADD INDEX idx_tasks_user_id (user_id);
```

---

## REST API Endpoints

Base path: `/api/v1`

### 1. Authentication Endpoints

| Method | Endpoint | Auth | Description | Status Codes |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register a new user and receive a JWT token | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `POST` | `/api/v1/auth/login` | Public | Authenticate credentials and receive a JWT token | `200 OK`, `400 Bad Request`, `401 Unauthorized` |
| `POST` | `/api/v1/auth/logout` | Public | Stateless logout confirmation (client discards token) | `200 OK` |

### 2. User Profile Endpoints

| Method | Endpoint | Auth | Description | Status Codes |
|---|---|---|---|---|
| `GET` | `/api/v1/users/me` | Bearer | Retrieve profile of the currently authenticated user | `200 OK`, `401 Unauthorized` |
| `PUT` | `/api/v1/users/me` | Bearer | Update first name and last name | `200 OK`, `400 Bad Request`, `401 Unauthorized` |

### 3. Task Management Endpoints (Owner-Isolated)

| Method | Endpoint | Auth | Description | Status Codes |
|---|---|---|---|---|
| `POST` | `/api/v1/tasks` | Bearer | Create a new task tied to the authenticated user | `201 Created`, `400 Bad Request`, `401 Unauthorized` |
| `GET` | `/api/v1/tasks` | Bearer | List owned tasks (paginated, sorted, and filtered) | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/v1/tasks/{id}` | Bearer | Retrieve a task by ID (returns 404 if owned by someone else) | `200 OK`, `401 Unauthorized`, `404 Not Found` |
| `PUT` | `/api/v1/tasks/{id}` | Bearer | Update entire task details | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `PATCH`| `/api/v1/tasks/{id}/status` | Bearer | Partially update only task status (`TODO`, `IN_PROGRESS`, `COMPLETED`) | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE`| `/api/v1/tasks/{id}` | Bearer | Delete an owned task | `204 No Content`, `404 Not Found` |
| `GET` | `/api/v1/tasks/search?title=keyword` | Bearer | Case-insensitive title search (paginated) | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/v1/tasks/overdue` | Bearer | Retrieve overdue tasks (past due date and not completed) | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/v1/tasks/due-between?startDate=...&endDate=...` | Bearer | Retrieve tasks due within date range | `200 OK`, `400 Bad Request`, `401 Unauthorized` |

---

## Sample Requests & Responses

### 1. User Registration (`POST /api/v1/auth/register`)
**Request Body:**
```json
{
  "email": "sarvam@example.com",
  "password": "SecurePassword123!",
  "firstName": "Sarvam",
  "lastName": "Kumar"
}
```
**Response (`201 Created`):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYXJ2YW1AZXhhbXBsZS5jb20iLCJpYXQiOjE3Mzk3...",
  "user": {
    "id": 1,
    "email": "sarvam@example.com",
    "firstName": "Sarvam",
    "lastName": "Kumar",
    "role": "ROLE_USER",
    "createdAt": "2026-10-17T01:15:30"
  }
}
```

### 2. User Login (`POST /api/v1/auth/login`)
**Request Body:**
```json
{
  "email": "sarvam@example.com",
  "password": "SecurePassword123!"
}
```
**Response (`200 OK`):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYXJ2YW1AZXhhbXBsZS5jb20iLCJpYXQiOjE3Mzk3...",
  "user": {
    "id": 1,
    "email": "sarvam@example.com",
    "firstName": "Sarvam",
    "lastName": "Kumar",
    "role": "ROLE_USER",
    "createdAt": "2026-10-17T01:15:30"
  }
}
```

### 3. Create Task (`POST /api/v1/tasks`)
**Header:** `Authorization: Bearer <token>`
**Request Body:**
```json
{
  "title": "Prepare Spring Boot Architecture Presentation",
  "description": "Cover Dependency Injection, Security Filter Chain, and JPA mapping",
  "priority": "HIGH",
  "dueDate": "2026-10-31"
}
```
**Response (`201 Created`):**
```json
{
  "id": 1,
  "title": "Prepare Spring Boot Architecture Presentation",
  "description": "Cover Dependency Injection, Security Filter Chain, and JPA mapping",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-10-31",
  "createdAt": "2026-10-17T01:25:00",
  "updatedAt": "2026-10-17T01:25:00"
}
```

### 4. Paginated, Sorted & Filtered Listing (`GET /api/v1/tasks`)
**Request:**
`GET /api/v1/tasks?status=TODO&priority=HIGH&page=0&size=10&sortBy=dueDate&direction=asc`
**Header:** `Authorization: Bearer <token>`
**Response (`200 OK`):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Prepare Spring Boot Architecture Presentation",
      "description": "Cover Dependency Injection, Security Filter Chain, and JPA mapping",
      "status": "TODO",
      "priority": "HIGH",
      "dueDate": "2026-10-31",
      "createdAt": "2026-10-17T01:25:00",
      "updatedAt": "2026-10-17T01:25:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 1,
  "totalPages": 1,
  "isFirst": true,
  "isLast": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 5. Validation Error Envelope (`400 Bad Request`)
**Request Body:** `{"title": ""}`
**Response (`400 Bad Request`):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/v1/tasks",
  "timestamp": "2026-10-17T01:26:00",
  "validationErrors": {
    "title": "Task title is required and cannot be blank"
  }
}
```

### 6. Authentication Failure Envelope (`401 Unauthorized`)
**Request:** `GET /api/v1/tasks` (Missing or invalid token)
**Response (`401 Unauthorized`):**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication token is missing or invalid",
  "path": "/api/v1/tasks"
}
```

---

## Getting Started & Local Setup

### Prerequisites
- **Java 21 JDK** or later (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- **MySQL 8.x** running locally or via Docker

### 1. Environment Configuration
Copy `.env.example` to `.env` or set environment variables in your terminal/IDE:
```bash
cp .env.example .env
```
Default properties in `.env.example`:
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=taskflow_db
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
SERVER_PORT=8080
JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION=86400000
```

### 2. Build and Run Application
```bash
# Clean and run with Maven
mvn spring-boot:run
```
The application will boot on `http://localhost:8080`. Flyway will automatically execute migrations `V1` and `V2` against MySQL.

### 3. Run Automated Tests
```bash
mvn test
```
Executes all 42 tests against the isolated in-memory test database with zero external dependencies required.

---

## Interactive Documentation (Swagger UI)

TaskFlow includes preconfigured Springdoc OpenAPI 3.0 documentation:

* **Swagger UI Web Portal:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Raw OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Authenticating in Swagger UI:
1. Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
2. Execute `POST /api/v1/auth/login` or `POST /api/v1/auth/register` and copy the returned `token`.
3. Click the green **Authorize** button at the top-right.
4. Paste your token into the **Value** box and click **Authorize**.
5. All protected endpoints are now ready for testing directly in your browser.

---

## Postman Collection

A complete, automated Postman test collection is included in the project:
* [`TaskFlow.postman_collection.json`](file:///C:/Users/SARVAM/OneDrive/Documents/Java_projects/TaskFlow/TaskFlow.postman_collection.json)
* [`postman/TaskFlow.postman_collection.json`](file:///C:/Users/SARVAM/OneDrive/Documents/Java_projects/TaskFlow/postman/TaskFlow.postman_collection.json)

### Importing & Testing:
1. Open Postman $\to$ Click **Import** $\to$ Select `TaskFlow.postman_collection.json`.
2. Execute **Register User A** or **Login User A**.
3. The Postman test script automatically stores the returned JWT token into the `{{jwt_token}}` collection variable.
4. All subsequent requests in the collection automatically include `Authorization: Bearer {{jwt_token}}`.
5. Run the **Security & Negative Tests** folder to verify that unauthenticated calls return `401`, blank fields return `400`, duplicate emails return `409`, and cross-user task access returns `404`.

---

## Docker & Containerized Deployment

TaskFlow provides a multi-stage `Dockerfile` and a `docker-compose.yml` service definition for zero-install deployment.

### 1. Build and Run Full Stack (App + MySQL)
```bash
docker compose up --build
```
This command:
1. Starts an official **MySQL 8.0** container with persistent storage volume (`mysql_data`).
2. Performs a healthcheck ping (`mysqladmin ping`) to ensure MySQL is ready.
3. Builds the Spring Boot application using a multi-stage Docker build.
4. Starts the application with a non-root user (`appuser`) and connects automatically to the MySQL container.

### 2. Stop Containers
```bash
docker compose down
```

---

## Automated Testing Suite

The testing suite contains **42 automated tests** across all architectural tiers:

| Test Class | Type | What is Tested |
|---|---|---|
| `TaskServiceTest` | Mockito Unit Test | Business logic, task creation ownership injection, status update, deletion guard |
| `UserServiceTest` | Mockito Unit Test | BCrypt password hashing, registration, duplicate email prevention, SecurityContext profile |
| `TaskControllerTest` | `@WebMvcTest` Slice | Routing, HTTP 201/200/204/400/404 statuses, JSON serialization, validation errors |
| `AuthControllerTest` | `@WebMvcTest` Slice | Register/Login endpoints, 201 Created, 401 Bad Credentials, 409 Conflict |
| `UserControllerTest` | `@WebMvcTest` Slice | Profile retrieval, profile update, input validation |
| `WelcomeControllerTest`| `@WebMvcTest` Slice | Public welcome endpoint |
| `TaskRepositoryTest` | `@DataJpaTest` Slice | Schema mapping, `@PrePersist` timestamps, JPQL queries, status & overdue filters |
| `UserRepositoryTest` | `@DataJpaTest` Slice | Email lookup, existence checks, unique constraint enforcement |
| `SecurityIntegrationTest` | `@SpringBootTest` | 401 on unauthenticated access, public auth bypass, valid JWT access, multi-tenant isolation |
| `TaskIntegrationTest` | `@SpringBootTest` | End-to-end task CRUD lifecycle, search, pagination, overdue queries, `@FutureOrPresent` date validation |
| `TaskFlowApplicationTests` | `@SpringBootTest` | Context bootstrap health |

---

## Annotations Master Reference

### Spring Boot & Dependency Injection
* `@SpringBootApplication`: Meta-annotation combining `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
* `@Configuration`: Identifies a class providing Spring bean definitions.
* `@Bean`: Directs Spring to manage the returned instance in the `ApplicationContext`.
* `@Component`, `@Service`, `@Repository`: Stereotype annotations indicating business components, services, and DAO/persistence classes.
* `@Value`: Injects configuration property values from `application.properties` or environment variables with optional defaults.

### REST APIs & Web Layer
* `@RestController`: Combines `@Controller` and `@ResponseBody`, serializing return values directly into JSON.
* `@RequestMapping("/api/v1/tasks")`: Sets the base URL path for all endpoints in the controller.
* `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`: HTTP method route bindings.
* `@PathVariable`: Extracts dynamic segments from URI templates (e.g., `/tasks/{id}`).
* `@RequestParam`: Extracts HTTP query parameters (e.g., `?status=TODO&page=0`).
* `@RequestBody`: Binds and deserializes incoming JSON payloads to Java DTOs.
* `@ResponseStatus`: Configures default HTTP response status codes.

### Persistence & JPA
* `@Entity`: Marks a class as a persistent JPA database entity.
* `@Table(name = "tasks")`: Maps the entity to the corresponding database table.
* `@Id` & `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Declares auto-increment primary key.
* `@Column`: Maps object fields to table columns with length, uniqueness, and nullability constraints.
* `@ManyToOne(fetch = FetchType.LAZY)`: Defines a many-to-one relationship with on-demand lazy loading.
* `@JoinColumn(name = "user_id")`: Specifies the foreign key column in the owning entity table.
* `@Enumerated(EnumType.STRING)`: Persists enums as readable text strings rather than ordinal integers.
* `@PrePersist` & `@PreUpdate`: Entity lifecycle callback hooks populating timestamps before SQL insert/update.
* `@Transactional`: Wraps method execution in database transactions with automatic commit on success and rollback on exceptions.

### Validation & Centralized Exceptions
* `@Valid`: Triggers Jakarta Bean Validation on incoming request bodies.
* `@NotBlank`: Rejects `null`, empty string `""`, or whitespace-only inputs.
* `@NotNull`: Rejects `null` values.
* `@Size(min, max)`: Enforces string or collection length constraints.
* `@Email`: Enforces RFC-compliant email formatting.
* `@FutureOrPresent`: Guarantees a date is today or in the future.
* `@RestControllerAdvice`: Intercepts exceptions thrown across all `@RestController` classes.
* `@ExceptionHandler(Type.class)`: Directs specific exception types to dedicated error-formatting methods.

### Security & Testing
* `@EnableWebSecurity`: Activates Spring Security's web protection filter chain.
* `@SpringBootTest`: Boots the entire application context for integration testing.
* `@WebMvcTest(Controller.class)`: Boots a lightweight web slice testing controllers in isolation.
* `@DataJpaTest`: Boots only JPA components and repositories against an in-memory database.
* `@MockitoBean`: Modern Spring Boot 3.4+ annotation registering Mockito mocks inside the Spring context.
* `@AutoConfigureMockMvc`: Injects `MockMvc` for simulating HTTP requests without booting a real HTTP server.
* `@ActiveProfiles("test")`: Activates `application-test.properties` for test isolation.

---

## Interview Questions & Answers Guide

### 1. Spring Boot & Architecture
**Q1: Why is Constructor Injection preferred over Field Injection (`@Autowired`)?**  
**Answer:**  
1. **Immutability:** Dependencies can be marked `final`, ensuring thread safety and preventing post-construction mutation.
2. **Clear Contract:** Required dependencies are made explicit at object instantiation time.
3. **Ease of Testing:** Unit tests can inject mock dependencies directly via the constructor without needing reflection or a Spring container.
4. **Circular Dependency Detection:** Spring detects circular dependencies at startup rather than encountering errors at runtime.

**Q2: What is the purpose of `@Transactional` and how does rollback work?**  
**Answer:** `@Transactional` manages database transactions declaratively using Spring AOP proxies. By default, Spring rolls back the transaction on **unchecked exceptions** (`RuntimeException` and `Error`), while committing on checked exceptions (`Exception`) unless explicitly configured with `@Transactional(rollbackFor = Exception.class)`.

---

### 2. Spring Security & JWT
**Q3: How does JWT authentication maintain statelessness?**  
**Answer:** A JWT is self-contained. It contains the claims (e.g., subject/email, expiration, roles) and a cryptographic signature. On every request, the server verifies the signature using the private secret key. No session state or database lookup is required to verify the user's identity, allowing horizontal scaling across multiple application instances without shared session stores.

**Q4: How does BCrypt differ from SHA-256 for password hashing?**  
**Answer:** SHA-256 is a general-purpose, fast cryptographic hash designed for high-throughput checksums. It is vulnerable to brute-force attacks and rainbow table lookups. BCrypt is a slow, computationally intensive hashing function that incorporates an automatic random salt (thwarting rainbow tables) and an adjustable work factor (cost) that can be increased as hardware improves.

**Q5: What is Insecure Direct Object Reference (IDOR) and how does TaskFlow prevent it?**  
**Answer:** IDOR occurs when an application exposes a reference to an internal resource (such as `/tasks/42`) and fails to verify that the requesting user owns that resource. TaskFlow prevents IDOR by deriving user identity strictly from `SecurityContextHolder` and executing queries scoped by user (`findByIdAndUser(id, currentUser)`). If User B requests User A's task, the repository returns empty, and the service throws `ResourceNotFoundException` (`404 Not Found`).

---

### 3. JPA & Hibernate
**Q6: What is the N+1 SELECT problem in JPA and how is it resolved?**  
**Answer:** The N+1 problem occurs when querying $N$ parent entities results in 1 query for the parents and $N$ additional queries to fetch their related children (e.g. loading tasks and executing a separate query for each task's user). It is resolved by using **`JOIN FETCH`** in JPQL, **`@EntityGraph`**, or `FetchType.LAZY` with DTO projections.

**Q7: Why should entities never be returned directly in REST API responses?**  
**Answer:**  
1. **Security:** Entities may expose sensitive fields (password hashes, internal IDs, audit data).
2. **Infinite Recursion:** Bidirectional relationships cause circular Jackson serialization loops (`StackOverflowError`).
3. **Coupling:** Changes to database columns immediately break frontend contracts. DTOs decouple external API contracts from internal database persistence.

---

## Troubleshooting Guide

| Issue | Cause | Solution |
|---|---|---|
| `Access denied for user 'root'@'localhost'` | Incorrect MySQL password | Update `DB_PASSWORD` in `.env` or set system environment variable `DB_PASSWORD`. |
| `Communications link failure` | MySQL service is not running | Start MySQL service via Windows Services (`services.msc`) or run `docker compose up -d mysql`. |
| `FlywayException: Validate failed: migration checksum mismatch` | A migration file was edited after being applied | Never edit applied migration files. Roll back locally or run `mvn flyway:repair`. |
| `401 Unauthorized` on protected endpoints | Missing or expired JWT token | Call `POST /api/v1/auth/login`, copy `token`, and pass `Authorization: Bearer <token>` in headers. |
| `404 Not Found` when accessing valid task ID | Task belongs to a different user | In TaskFlow, you can only access tasks you created. Verify that you are authenticated as the task owner. |
| `Port 8080 is already in use` | Another process is occupying port 8080 | Change `SERVER_PORT=8081` in `.env` or terminate the existing process (`Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process`). |

---

## Limitations & Production Readiness

While TaskFlow is architected with enterprise patterns, a full production deployment at high scale should consider the following enhancements:

1. **Server-Side Token Revocation:** Stateless JWTs remain valid until their expiration timestamp. Implementing immediate logout requires a token blacklist store (e.g. **Redis** cache).
2. **Refresh Tokens:** Access tokens currently have a 24-hour expiration. An enterprise setup typically uses a 15-minute access token combined with a rotating, secure HTTP-only cookie refresh token.
3. **Email Verification:** User registration currently activates accounts immediately without requiring email confirmation links.
4. **Rate Limiting:** Protect public endpoints (`/auth/login`, `/auth/register`) against brute-force attacks using Bucket4j or an API Gateway.
5. **CORS Origins:** Configure explicit trusted frontend origins in `SecurityConfig.java` rather than allowing defaults.

---

## License

This project is licensed under the Apache License 2.0. Built for learning, pair-programming, and Java backend technical interview preparation.
