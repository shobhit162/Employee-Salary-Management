package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.compensation.dto.CompensationResponse;
import org.springframework.stereotype.Component;

@Component
public class CompensationMapper {

    public CompensationResponse toResponse(
            Compensation compensation
    ) {
        return new CompensationResponse(
                compensation.getId(),
                compensation.getEmployee().getId(),
                compensation.getAmount(),
                compensation.getCurrency(),
                compensation.getEffectiveFrom(),
                compensation.getEffectiveTo()
        );
    }
}