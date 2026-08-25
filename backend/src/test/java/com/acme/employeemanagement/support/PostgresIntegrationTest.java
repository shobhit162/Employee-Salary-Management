package com.acme.employeemanagement.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Boots the whole application against a real PostgreSQL container.
 *
 * <p>These tests exist because the parts they cover cannot be verified any other
 * way: Flyway migrations, the {@code btree_gist} exclusion constraint that stops
 * salary periods overlapping, and the analytics SQL, which uses PostgreSQL
 * features an in-memory database does not have.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("integration-test")
@ExtendWith(DockerAvailableCondition.class)
@Import(PostgresTestContainer.class)
public @interface PostgresIntegrationTest {
}
