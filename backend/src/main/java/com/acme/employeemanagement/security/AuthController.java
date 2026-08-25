package com.acme.employeemanagement.security;

import com.acme.employeemanagement.security.dto.AuthenticatedUserResponse;
import com.acme.employeemanagement.security.dto.LoginRequest;
import com.acme.employeemanagement.security.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                )
        );

        JwtTokenService.IssuedToken issued = jwtTokenService.issue(authentication);

        return new LoginResponse(
                issued.token(),
                issued.expiresAt(),
                authentication.getName(),
                issued.roles()
        );
    }

    /**
     * Lets the client confirm a stored token is still valid on page load,
     * instead of discovering it expired on the first data request.
     */
    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        return new AuthenticatedUserResponse(
                authentication.getName(),
                Roles.of(authentication),
                jwt.getExpiresAt()
        );
    }
}
