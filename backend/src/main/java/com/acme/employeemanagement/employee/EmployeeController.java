package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.employee.dto.CreateEmployeeRequest;
import com.acme.employeemanagement.employee.dto.EmployeeResponse;
import com.acme.employeemanagement.employee.dto.UpdateEmployeeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        EmployeeResponse response = employeeService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{employeeId}")
    public EmployeeResponse getById(
            @PathVariable UUID employeeId
    ) {
        return employeeService.getById(employeeId);
    }

    @GetMapping
    public Page<EmployeeResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        int validatedSize = Math.min(
                Math.max(size, 1),
                MAX_PAGE_SIZE
        );

        int validatedPage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                validatedPage,
                validatedSize,
                Sort.by(direction, sortBy)
        );

        return employeeService.search(
                search,
                countryCode,
                department,
                status,
                pageable
        );
    }

    @PutMapping("/{employeeId}")
    public EmployeeResponse update(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return employeeService.update(employeeId, request);
    }

    @PostMapping("/{employeeId}/termination")
    public EmployeeResponse terminate(
            @PathVariable UUID employeeId
    ) {
        return employeeService.terminate(employeeId);
    }
}