package com.FXplore.fx_rate_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base class for all integration tests (*IT.java).
 *
 * Uses the TRUE Testcontainers Singleton pattern:
 *   - The container is declared as a static field and started in a static initialiser.
 *   - This means ONE container is started for the entire JVM / Maven build, regardless
 *     of how many IT subclasses exist.  The JVM shutdown hook (Ryuk) stops it at the end.
 *   - We intentionally do NOT use @Testcontainers + @Container here, because that
 *     annotation pair ties the container lifecycle to the test-class lifecycle — the
 *     container would be stopped after the first class finishes and the second class
 *     would get "Connection refused".
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
public abstract class AbstractIntegrationTest {

    /**
     * Single MySQL 8.0 container shared by ALL IT subclasses in the same JVM run.
     *
     * Started eagerly in the static initialiser block below — not by @Container —
     * so it is never stopped between test classes.
     */
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("fx_test")
                    .withUsername("test")
                    .withPassword("test");

    // Start the container once for the whole test suite.
    // Testcontainers registers a JVM shutdown hook (via Ryuk) to stop it automatically.
    static {
        MYSQL.start();
    }

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
        // Keep pool size at the minimum valid value (scheduler bean must exist but
        // should not fire during tests — disabled via @ConditionalOnProperty).
        registry.add("spring.task.scheduling.pool.size",    () -> "1");
        registry.add("app.scheduling.enabled",              () -> "false");
    }
}
