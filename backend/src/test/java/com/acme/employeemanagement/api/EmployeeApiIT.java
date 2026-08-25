package com.acme.employeemanagement.api;

import com.acme.employeemanagement.support.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the HTTP contract end to end: log in, hire someone, pay them, schedule a
 * raise, cancel it, then terminate them.
 *
 * <p>Deliberately not {@code @Transactional} — each request runs on its own
 * transaction, exactly as it will in production, so a rule that only holds
 * inside a single transaction would fail here.
 */
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeApiIT extends IntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    private String bearerToken;

    @BeforeAll
    void logIn() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "hr.manager@acme.test",
                                  "password": "test-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("HR_MANAGER"))
                .andReturn();

        bearerToken = "Bearer " + read(result).get("token").asText();
    }

    @Test
    @DisplayName("salary data is not reachable without a token")
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("bad credentials are refused")
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "hr.manager@acme.test",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("hire, pay, schedule a raise, cancel it, then terminate")
    void walksTheFullEmployeeLifecycle() throws Exception {
        String employeeId = createEmployee("api-lifecycle");

        // A brand new employee can be paid from today.
        mockMvc.perform(authorised(post(
                "/api/v1/employees/%s/compensations".formatted(employeeId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100000,
                                  "currency": "USD",
                                  "effectiveFrom": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100000))
                .andExpect(jsonPath("$.effectiveTo").doesNotExist());

        MvcResult raise = mockMvc.perform(authorised(post(
                "/api/v1/employees/%s/compensations".formatted(employeeId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 130000,
                                  "currency": "USD",
                                  "effectiveFrom": "%s"
                                }
                                """.formatted(LocalDate.now().plusMonths(1))))
                .andExpect(status().isCreated())
                .andReturn();

        String raiseId = read(raise).get("id").asText();

        mockMvc.perform(authorised(get(
                "/api/v1/employees/%s/compensations".formatted(employeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.amount").value(100000))
                .andExpect(jsonPath("$.scheduled.amount").value(130000))
                .andExpect(jsonPath("$.history.length()").value(2));

        // Termination is blocked while a raise is pending...
        mockMvc.perform(authorised(post(
                "/api/v1/employees/%s/termination".formatted(employeeId))))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(authorised(delete(
                "/api/v1/employees/%s/compensations/%s"
                        .formatted(employeeId, raiseId))))
                .andExpect(status().isNoContent());

        // ...and the cancelled raise leaves the current salary open-ended.
        mockMvc.perform(authorised(get(
                "/api/v1/employees/%s/compensations".formatted(employeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.amount").value(100000))
                .andExpect(jsonPath("$.current.effectiveTo").doesNotExist())
                .andExpect(jsonPath("$.scheduled").doesNotExist());

        mockMvc.perform(authorised(post(
                "/api/v1/employees/%s/termination".formatted(employeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("TERMINATED"));

        // Salary history survives termination.
        mockMvc.perform(authorised(get(
                "/api/v1/employees/%s/compensations".formatted(employeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(1));
    }

    @Test
    @DisplayName("a duplicate employee code is a conflict, not a crash")
    void rejectsDuplicateEmployeeCode() throws Exception {
        createEmployee("api-duplicate");

        mockMvc.perform(authorised(post("/api/v1/employees"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("api-duplicate")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"));
    }

    @Test
    @DisplayName("invalid input is reported field by field")
    void reportsValidationErrors() throws Exception {
        mockMvc.perform(authorised(post("/api/v1/employees"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeCode": "",
                                  "firstName": "A",
                                  "lastName": "B",
                                  "email": "not-an-email",
                                  "countryCode": "USA",
                                  "department": "Engineering",
                                  "jobTitle": "Engineer"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("an unsupported sort field is a client error, not a 500")
    void rejectsUnknownSortField() throws Exception {
        mockMvc.perform(authorised(get("/api/v1/employees"))
                        .param("sortBy", "salary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the list carries each employee's current salary")
    void listsEmployeesWithTheirCurrentSalary() throws Exception {
        String employeeId = createEmployee("api-listing");

        mockMvc.perform(authorised(post(
                "/api/v1/employees/%s/compensations".formatted(employeeId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 90000,
                                  "currency": "EUR",
                                  "effectiveFrom": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(authorised(get("/api/v1/employees"))
                        .param("search", "api-listing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].currentCompensation.amount")
                        .value(90000))
                .andExpect(jsonPath("$.content[0].currentCompensation.currency")
                        .value("EUR"));
    }

    @Test
    @DisplayName("analytics answers with headline pay figures")
    void reportsOrganisationPay() throws Exception {
        mockMvc.perform(authorised(get("/api/v1/analytics/summary"))
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.statistics.employeeCount").exists());

        mockMvc.perform(authorised(get("/api/v1/analytics/breakdown"))
                        .param("dimension", "DEPARTMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimension").value("DEPARTMENT"));

        mockMvc.perform(authorised(get("/api/v1/analytics/breakdown"))
                        .param("dimension", "NOT_A_DIMENSION"))
                .andExpect(status().isBadRequest());
    }

    private String createEmployee(String code) throws Exception {
        MvcResult result = mockMvc.perform(authorised(post("/api/v1/employees"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(code)))
                .andExpect(status().isCreated())
                .andReturn();

        return read(result).get("id").asText();
    }

    private static String employeeJson(String code) {
        return """
                {
                  "employeeCode": "%s",
                  "firstName": "Test",
                  "lastName": "Person",
                  "email": "%s@acme.test",
                  "countryCode": "US",
                  "department": "Engineering",
                  "jobTitle": "Software Engineer"
                }
                """.formatted(code, code);
    }

    private MockHttpServletRequestBuilder authorised(
            MockHttpServletRequestBuilder request
    ) {
        return request.header(HttpHeaders.AUTHORIZATION, bearerToken);
    }

    private static JsonNode read(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }
}
