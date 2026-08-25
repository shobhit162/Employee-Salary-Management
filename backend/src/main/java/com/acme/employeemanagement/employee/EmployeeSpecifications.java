package com.acme.employeemanagement.employee;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class EmployeeSpecifications {

    /** Bounds a pathological query: each term adds four LIKE predicates. */
    private static final int MAX_SEARCH_TERMS = 5;

    private static final char LIKE_ESCAPE = '!';

    private EmployeeSpecifications() {
    }

    /**
     * Free-text search across employee code, name and email.
     *
     * <p>The query is split into terms and each term must match <em>some</em>
     * field, rather than the whole phrase having to appear in a single one.
     * Without that, "Cadila Abbott" is matched as one string against the first
     * name and against the last name separately, and finds nobody — even though
     * the employee exists. Splitting also makes the order irrelevant, so
     * "Abbott Cadila" works too.
     */
    public static Specification<Employee> search(String search) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return criteriaBuilder.conjunction();
            }

            List<String> patterns = Arrays.stream(search.trim().split("\\s+"))
                    .filter(term -> !term.isEmpty())
                    .limit(MAX_SEARCH_TERMS)
                    .map(EmployeeSpecifications::likePattern)
                    .toList();

            if (patterns.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            Predicate[] everyTermMatches = patterns.stream()
                    .map(pattern -> criteriaBuilder.or(
                            like(criteriaBuilder, root.get("employeeCode"), pattern),
                            like(criteriaBuilder, root.get("firstName"), pattern),
                            like(criteriaBuilder, root.get("lastName"), pattern),
                            like(criteriaBuilder, root.get("email"), pattern)
                    ))
                    .toArray(Predicate[]::new);

            return criteriaBuilder.and(everyTermMatches);
        };
    }

    private static Predicate like(
            CriteriaBuilder criteriaBuilder,
            Path<String> field,
            String pattern
    ) {
        return criteriaBuilder.like(
                criteriaBuilder.lower(field),
                pattern,
                LIKE_ESCAPE
        );
    }

    /**
     * Wraps a term in wildcards, escaping any the user typed themselves — a bare
     * {@code %} would otherwise match every employee.
     */
    private static String likePattern(String term) {
        String escaped = term.toLowerCase(Locale.ROOT)
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");

        return "%" + escaped + "%";
    }

    public static Specification<Employee> hasCountry(String countryCode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(countryCode)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("countryCode"),
                    countryCode.trim().toUpperCase(Locale.ROOT)
            );
        };
    }

    public static Specification<Employee> hasDepartment(String department) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(department)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("department")),
                    department.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    public static Specification<Employee> hasStatus(
            EmploymentStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    root.get("employmentStatus"),
                    status
            );
        };
    }
}