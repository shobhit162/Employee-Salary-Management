package com.acme.employeemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * Reads the caller's roles out of their granted authorities.
 *
 * <p>Not every authority is a role: Spring Security also grants
 * authentication-factor authorities such as {@code FACTOR_PASSWORD}, which
 * record how someone signed in rather than what they are allowed to do. Only
 * {@code ROLE_}-prefixed authorities are treated as roles, so those never leak
 * into a token or an API response.
 */
final class Roles {

    static final String PREFIX = "ROLE_";

    private Roles() {
    }

    static List<String> of(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(PREFIX))
                .map(authority -> authority.substring(PREFIX.length()))
                .toList();
    }
}
