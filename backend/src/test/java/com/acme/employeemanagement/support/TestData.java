package com.acme.employeemanagement.support;

import com.acme.employeemanagement.compensation.Compensation;
import com.acme.employeemanagement.compensation.CompensationRepository;
import com.acme.employeemanagement.employee.Employee;
import com.acme.employeemanagement.employee.EmployeeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Builds the small, readable fixtures the integration tests work with. */
public class TestData {

    private final AtomicInteger sequence = new AtomicInteger();

    private final EmployeeRepository employeeRepository;
    private final CompensationRepository compensationRepository;

    public TestData(
            EmployeeRepository employeeRepository,
            CompensationRepository compensationRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.compensationRepository = compensationRepository;
    }

    public Employee employee(String countryCode, String department) {
        int index = sequence.incrementAndGet();

        return employeeRepository.saveAndFlush(new Employee(
                "TEST-%04d".formatted(index),
                "First" + index,
                "Last" + index,
                "employee%d@acme.test".formatted(index),
                countryCode,
                department,
                "Software Engineer"
        ));
    }

    public Employee terminatedEmployee(String countryCode, String department) {
        Employee employee = employee(countryCode, department);
        employee.terminate(LocalDate.now().minusDays(1));

        return employeeRepository.saveAndFlush(employee);
    }

    public Compensation salary(
            Employee employee,
            String amount,
            String currency,
            LocalDate from
    ) {
        return salary(employee, amount, currency, from, null);
    }

    public Compensation salary(
            Employee employee,
            String amount,
            String currency,
            LocalDate from,
            LocalDate to
    ) {
        // Flushed immediately so the analytics SQL, which bypasses the JPA
        // session, sees the fixture inside the test transaction.
        return compensationRepository.saveAndFlush(new Compensation(
                employee,
                new BigDecimal(amount),
                currency,
                from,
                to
        ));
    }
}
