package com.acme.employeemanagement.compensation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
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

    /**
     * The salaries effective on {@code date} for a whole page of employees.
     *
     * <p>Fetching these in one query keeps listing employees at two queries per
     * page regardless of page size, instead of one per row.
     */
    @Query("""
            select c
            from Compensation c
            where c.employee.id in :employeeIds
              and c.effectiveFrom <= :date
              and (c.effectiveTo is null or c.effectiveTo > :date)
            """)
    List<Compensation> findEffectiveCompensations(
            @Param("employeeIds") Collection<UUID> employeeIds,
            @Param("date") LocalDate date
    );

    /**
     * Finds the period that was closed to make room for the compensation starting
     * on {@code effectiveTo}. Used to restore salary coverage when a scheduled
     * change is cancelled.
     */
    @Query("""
            select c
            from Compensation c
            where c.employee.id = :employeeId
              and c.effectiveTo = :effectiveTo
            """)
    Optional<Compensation> findPeriodEndingOn(
            @Param("employeeId") UUID employeeId,
            @Param("effectiveTo") LocalDate effectiveTo
    );

    boolean existsByEmployeeIdAndEffectiveFromAfter(
            UUID employeeId,
            LocalDate date
    );

    /**
     * Currencies in active use, so analytics can fail loudly on a missing
     * exchange rate instead of silently dropping salaries from the totals.
     */
    @Query("""
            select distinct c.currency
            from Compensation c
            """)
    List<String> findDistinctCurrencies();
}
