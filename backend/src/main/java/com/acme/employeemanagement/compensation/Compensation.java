package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compensations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Compensation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Compensation(
            Employee employee,
            BigDecimal amount,
            String currency,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        this.employee = employee;
        this.amount = amount;
        this.currency = currency;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public void closeAt(LocalDate effectiveTo) {
        if (effectiveTo == null) {
            throw new IllegalArgumentException(
                    "Effective-to date cannot be null when closing compensation"
            );
        }

        if (!effectiveTo.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException(
                    "Effective-to date must be after effective-from date"
            );
        }

        if (this.effectiveTo != null) {
            throw new IllegalStateException(
                    "Compensation period is already closed"
            );
        }

        this.effectiveTo = effectiveTo;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}