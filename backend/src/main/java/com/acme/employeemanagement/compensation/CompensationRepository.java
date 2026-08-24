package com.acme.employeemanagement.compensation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompensationRepository
        extends JpaRepository<Compensation, UUID> {

    @Query("""
            select c
            from Compensation c
            where c.employee.id = :employeeId
              and c.effectiveFrom <= :date
              and (c.effectiveTo is null or c.effectiveTo > :date)
            """)
    Optional<Compensation> findEffectiveCompensation(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
    );

    @Query("""
            select c
            from Compensation c
            where c.employee.id = :employeeId
            order by c.effectiveFrom desc
            """)
    List<Compensation> findHistory(
            @Param("employeeId") UUID employeeId
    );

    @Query("""
            select c
            from Compensation c
            where c.employee.id = :employeeId
              and c.effectiveFrom > :date
            order by c.effectiveFrom asc
            """)
    List<Compensation> findScheduledCompensations(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
    );

    boolean existsByEmployeeIdAndEffectiveFromAfter(
            UUID employeeId,
            LocalDate date
    );
}