package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the TaskFlow Spring Boot application.
 *
 * Annotations explained:
 * @SpringBootApplication: A meta-annotation that combines:
 *  1. @Configuration: Tags the class as a source of bean definitions.
 *  2. @EnableAutoConfiguration: Tells Spring Boot to start adding beans based on classpath settings.
 *  3. @ComponentScan: Tells Spring to look for other components, configurations, and services in the com.taskflow package.
 */
@SpringBootApplication
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }

}
