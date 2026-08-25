package com.acme.employeemanagement.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication settings, configured under {@code app.security}.
 *
 * <p>Accounts live in configuration rather than in the database. The product has
 * a single persona — the HR Manager — and a handful of named accounts, so a user
 * table, invitations and password resets would be scope without a customer.
 * Swapping this for a real identity provider later means replacing the
 * {@code UserDetailsService} bean, not touching the rest of the application.
 */
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();

    private List<User> users = new ArrayList<>();

    /** Origins allowed to call the API from a browser, e.g. the Angular app. */
    private List<String> allowedOrigins = new ArrayList<>();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public static class Jwt {

        /**
         * HMAC signing secret. Must be at least 32 characters (256 bits) for
         * HS256. Always override this outside local development.
         */
        private String secret;

        private Duration ttl = Duration.ofHours(8);

        private String issuer = "acme-employee-management";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class User {

        private String username;

        /**
         * Either a plain password, which is hashed at startup and never stored,
         * or an already-encoded value such as {@code {bcrypt}$2a$10$...}.
         */
        private String password;

        private String role = "HR_MANAGER";

        private String displayName;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
