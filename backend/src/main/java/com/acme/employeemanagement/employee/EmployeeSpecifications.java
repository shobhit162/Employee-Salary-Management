package com.acme.employeemanagement.employee;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> search(String search) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("employeeCode")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("firstName")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("lastName")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("email")),
                            pattern
                    )
            );
        };
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