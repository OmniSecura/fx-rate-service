package com.FXplore.fx_rate_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests (*IT.java).
 *
 * Uses the Testcontainers Singleton pattern with the official JUnit 5 extension:
 *   - {@code @Testcontainers} activates the JUnit 5 extension that manages
 *     container lifecycle automatically (start before tests, stop after).
 *   - {@code @Container} on a <b>static</b> field means the container is shared
 *     across ALL test methods in ALL subclasses within the same JVM run
 *     (one container start per Maven build, not per test class).
 *
 * Schema and seed data are loaded automatically by Spring Boot via:
 *   src/test/resources/schema.sql  – DDL (tables, indexes, constraints)
 *   src/test/resources/data.sql    – reference / seed rows
 *
 * Subclasses should annotate individual test methods (or the whole class) with
 * {@code @Transactional} when they write data that must be rolled back after
 * the test, to keep each test isolated from the shared seed data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    /**
     * Single MySQL 8.0 container shared by all IT subclasses (Singleton pattern).
     *
     * Static + @Container = JUnit 5 extension starts it once before the first test
     * class that uses it and stops it after the last one finishes.
     * This is the recommended Testcontainers approach for Spring Boot integration tests.
     */
    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("fx_test")
                    .withUsername("test")
                    .withPassword("test");

    /**
     * Injects the Testcontainers-assigned JDBC URL, credentials and Hibernate settings
     * into the Spring application context before it boots.
     *
     * This overrides whatever is in application.properties so the app always
     * connects to the container rather than a real MySQL instance.
     * {@code spring.sql.init.mode=always} ensures schema.sql and data.sql
     * are executed on every fresh container start.
     */
    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",                MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",           MYSQL::getUsername);
        registry.add("spring.datasource.password",           MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto",       () -> "none");
        registry.add("spring.jpa.database-platform",        () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.sql.init.mode",                () -> "always");
        // Disable the scheduler during integration tests to avoid background noise in logs
        registry.add("spring.task.scheduling.pool.size",    () -> "0");
    }
}
