package com.acme.employeemanagement.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for tests that talk to a real database.
 *
 * <p>Every test starts from an empty schema. That matters because the API tests
 * deliberately commit — they exercise the same one-transaction-per-request
 * behaviour as production — so without this they would leave rows behind and the
 * next run would see duplicate employee codes and inflated headcounts.
 *
 * <p>For tests that are themselves {@code @Transactional} the truncate is rolled
 * back with the rest of the test, which leaves the database exactly as it was.
 */
@PostgresIntegrationTest
public abstract class IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("truncate table compensations, employees cascade");
    }
}
