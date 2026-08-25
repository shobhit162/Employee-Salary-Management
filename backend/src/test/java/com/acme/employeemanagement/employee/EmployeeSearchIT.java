package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.employee.dto.EmployeeListItemResponse;
import com.acme.employeemanagement.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Free-text search, against real SQL.
 *
 * <p>These run against the database because the bug they pin down was in the
 * generated predicate, not in any Java branch: searching a full name matched the
 * whole phrase against each column separately and returned nothing.
 */
@Transactional
class EmployeeSearchIT extends IntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void seedPeople() {
        save("ACME-00001", "Cadila", "Abbott", "cadila.abbott@acme.test");
        save("ACME-00002", "Priya", "Abbott", "priya.abbott@acme.test");
        save("ACME-00003", "Cadila", "Nguyen", "cadila.nguyen@acme.test");
        save("ACME-00004", "Marcus", "Silva", "marcus.silva@acme.test");
        employeeRepository.flush();
    }

    @Test
    @DisplayName("a full name finds the person, not nothing")
    void findsAnEmployeeByFullName() {
        assertThat(codesFor("Cadila Abbott")).containsExactly("ACME-00001");
    }

    @Test
    @DisplayName("the order of the terms does not matter")
    void ignoresTermOrder() {
        assertThat(codesFor("Abbott Cadila")).containsExactly("ACME-00001");
    }

    @Test
    @DisplayName("every term has to match something, so terms narrow the result")
    void narrowsAsTermsAreAdded() {
        assertThat(codesFor("Cadila"))
                .containsExactlyInAnyOrder("ACME-00001", "ACME-00003");
        assertThat(codesFor("Abbott"))
                .containsExactlyInAnyOrder("ACME-00001", "ACME-00002");
        assertThat(codesFor("Cadila Abbott")).containsExactly("ACME-00001");
    }

    @Test
    @DisplayName("partial terms still match")
    void matchesPartialTerms() {
        assertThat(codesFor("cad abb")).containsExactly("ACME-00001");
    }

    @Test
    @DisplayName("a single term still searches every field")
    void searchesCodeAndEmail() {
        assertThat(codesFor("ACME-00004")).containsExactly("ACME-00004");
        assertThat(codesFor("marcus.silva@acme.test")).containsExactly("ACME-00004");
    }

    @Test
    @DisplayName("search is case-insensitive and tolerates untidy spacing")
    void normalisesTheQuery() {
        assertThat(codesFor("  cAdIlA   ABBOTT  ")).containsExactly("ACME-00001");
    }

    @Test
    @DisplayName("a wildcard typed by the user is matched literally")
    void doesNotTreatPercentAsAWildcard() {
        // Without escaping, '%' matches every employee — a confusing result for
        // anyone who types one, and a cheap way to pull the whole table.
        assertThat(codesFor("%")).isEmpty();
        assertThat(codesFor("_")).isEmpty();
    }

    @Test
    @DisplayName("an empty query returns everyone")
    void treatsBlankAsNoFilter() {
        assertThat(codesFor("   ")).hasSize(4);
        assertThat(codesFor(null)).hasSize(4);
    }

    private List<String> codesFor(String search) {
        Page<EmployeeListItemResponse> page = employeeService.search(
                search,
                null,
                null,
                null,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "employeeCode"))
        );

        return page.getContent().stream()
                .map(row -> row.employee().employeeCode())
                .toList();
    }

    private void save(
            String code,
            String firstName,
            String lastName,
            String email
    ) {
        employeeRepository.save(new Employee(
                code,
                firstName,
                lastName,
                email,
                "US",
                "Engineering",
                "Software Engineer"
        ));
    }
}
