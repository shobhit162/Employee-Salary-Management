package com.acme.employeemanagement.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Skips database integration tests when Docker is not running.
 *
 * <p>The alternative — letting them fail — makes {@code mvn verify} unusable on a
 * machine without Docker and buries real failures in noise. Skipped tests are
 * reported as skipped, so the gap is visible rather than silent.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static volatile Boolean dockerAvailable;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(
            ExtensionContext context
    ) {
        if ("false".equals(System.getProperty(
                PostgresTestContainer.CONTAINER_ENABLED_PROPERTY
        ))) {
            return ConditionEvaluationResult.enabled(
                    "Running against an externally provided PostgreSQL"
            );
        }

        return isDockerAvailable()
                ? ConditionEvaluationResult.enabled("Docker is available")
                : ConditionEvaluationResult.disabled(
                        "Docker is not available - skipping database integration test"
                );
    }

    private static boolean isDockerAvailable() {
        Boolean cached = dockerAvailable;

        if (cached == null) {
            synchronized (DockerAvailableCondition.class) {
                cached = dockerAvailable;

                if (cached == null) {
                    cached = probeDocker();
                    dockerAvailable = cached;
                }
            }
        }

        return cached;
    }

    private static boolean probeDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable failure) {
            return false;
        }
    }
}
