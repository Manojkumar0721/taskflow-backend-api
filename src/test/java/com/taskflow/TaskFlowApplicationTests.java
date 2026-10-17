package com.taskflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic smoke test verifying the Spring ApplicationContext loads with the test profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskFlowApplicationTests {

    @Test
    @DisplayName("Application context should load successfully with test profile")
    void contextLoads() {
    }

}
