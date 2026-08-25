package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.employee.dto.CreateEmployeeRequest;
import com.acme.employeemanagement.employee.dto.EmployeeFilterOptionsResponse;
import com.acme.employeemanagement.employee.dto.EmployeeListItemResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Sorting maps straight onto entity properties, so the accepted values are
     * whitelisted rather than passed through — an unknown property would
     * otherwise surface as a 500.
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "employeeCode",
            "firstName",
            "lastName",
            "email",
            "countryCode",
            "department",
            "jobTitle",
            "employmentStatus"
    );

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

    @GetMapping("/filter-options")
    public EmployeeFilterOptionsResponse filterOptions() {
        return employeeService.filterOptions();
    }

    @GetMapping
    public Page<EmployeeListItemResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Cannot sort by '" + sortBy + "'. Sortable fields: "
                            + String.join(", ", new TreeSet<>(SORTABLE_FIELDS))
            );
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
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