package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.compensation.dto.CompensationResponse;
import com.acme.employeemanagement.compensation.dto.CompensationSummaryResponse;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/compensations")
@RequiredArgsConstructor
public class CompensationController {

    private final CompensationService compensationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompensationResponse create(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateCompensationRequest request
    ) {
        return compensationService.create(employeeId, request);
    }

    @GetMapping
    public CompensationSummaryResponse getSummary(
            @PathVariable UUID employeeId
    ) {
        return compensationService.getSummary(employeeId);
    }

    @DeleteMapping("/{compensationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelScheduledCompensation(
            @PathVariable UUID employeeId,
            @PathVariable UUID compensationId
    ) {
        compensationService.cancelScheduledCompensation(
                employeeId,
                compensationId
        );
    }
}
