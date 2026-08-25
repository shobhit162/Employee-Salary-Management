package com.acme.employeemanagement.employee;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository
        extends JpaRepository<Employee, UUID>,
                JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("""
            select distinct e.countryCode
            from Employee e
            order by e.countryCode
            """)
    List<String> findDistinctCountryCodes();

    @Query("""
            select distinct e.department
            from Employee e
            order by e.department
            """)
    List<String> findDistinctDepartments();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from Employee e
            where e.id = :employeeId
            """)
    Optional<Employee> findByIdForUpdate(
            @Param("employeeId") UUID employeeId
    );
}