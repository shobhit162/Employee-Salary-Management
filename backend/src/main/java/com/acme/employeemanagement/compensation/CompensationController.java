package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.compensation.dto.CompensationResponse;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/current")
    public CompensationResponse getCurrent(
            @PathVariable UUID employeeId
    ) {
        return compensationService.getCurrent(employeeId);
    }

    @GetMapping("/history")
    public List<CompensationResponse> getHistory(
            @PathVariable UUID employeeId
    ) {
        return compensationService.getHistory(employeeId);
    }
}