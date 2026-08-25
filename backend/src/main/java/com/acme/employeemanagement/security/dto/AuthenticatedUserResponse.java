package com.acme.employeemanagement.security.dto;

import java.time.Instant;
import java.util.List;

public record AuthenticatedUserResponse(
        String username,
        List<String> roles,
        Instant expiresAt
) {
}
