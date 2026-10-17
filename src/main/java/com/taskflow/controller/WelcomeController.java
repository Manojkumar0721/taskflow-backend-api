package com.taskflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller to provide application health and welcome/status endpoints.
 * Demonstrates the fundamentals of Spring REST controllers.
 */
@RestController
@RequestMapping("/api/v1")
public class WelcomeController {

    /**
     * Basic GET endpoint returning a JSON response.
     * Accessible via GET /api/v1/welcome
     */
    @GetMapping("/welcome")
    public ResponseEntity<Map<String, Object>> getWelcomeMessage() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "TaskFlow - Task Management System");
        response.put("version", "1.0.0");
        response.put("status", "UP");
        response.put("message", "Welcome to TaskFlow REST API!");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
