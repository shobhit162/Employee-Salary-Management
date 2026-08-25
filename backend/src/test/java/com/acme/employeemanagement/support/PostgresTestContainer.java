package com.acme.employeemanagement.support;

import com.acme.employeemanagement.compensation.CompensationRepository;
import com.acme.employeemanagement.employee.EmployeeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The database the integration tests run against.
 *
 * <p>By default a PostgreSQL container is started, shared across every test class
 * in the run via Spring's context cache, so the (slow) container start is paid
 * once rather than per class.
 *
 * <p>Set {@code -Dtest.postgres.container=false} together with the usual
 * {@code spring.datasource.*} properties to run against a PostgreSQL instance
 * that is already running — useful in CI where the database is a service
 * container, and on a developer machine without Docker.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {

    static final String CONTAINER_ENABLED_PROPERTY = "test.postgres.container";

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(
            name = CONTAINER_ENABLED_PROPERTY,
            havingValue = "true",
            matchIfMissing = true
    )
    @SuppressWarnings("resource")
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("employee_management")
                .withUsername("postgres")
                .withPassword("postgres");
    }

    @Bean
    TestData testData(
            EmployeeRepository employeeRepository,
            CompensationRepository compensationRepository
    ) {
        return new TestData(employeeRepository, compensationRepository);
    }
}
