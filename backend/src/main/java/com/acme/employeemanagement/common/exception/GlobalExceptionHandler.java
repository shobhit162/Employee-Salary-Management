package com.acme.employeemanagement.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Turns exceptions into RFC 9457 problem responses so the UI always gets a
 * predictable error shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(
            ResourceNotFoundException exception
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                exception.getMessage()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(
            DuplicateResourceException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Duplicate Resource",
                exception.getMessage()
        );
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(
            BusinessRuleViolationException exception
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Business Rule Violation",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .collect(Collectors.joining(", "));

        return problem(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                exception.getMessage()
        );
    }

    /**
     * An unparseable query parameter — an unknown enum value or a malformed date —
     * is a client mistake, not a server fault.
     */
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ProblemDetail handleBadParameter(Exception exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Request Parameter",
                exception.getMessage()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        // The database is the last line of defence for rules such as
        // non-overlapping salary periods; if it fires, a service check is missing.
        log.warn("Database constraint rejected an operation", exception);

        return problem(
                HttpStatus.CONFLICT,
                "Data Integrity Violation",
                "The requested operation violates a database constraint."
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException exception) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Invalid username or password"
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(
            AuthenticationException exception
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Authentication is required to access this resource"
        );
    }

    /**
     * Anything unhandled is logged in full but reported without internals, so a
     * stack trace never reaches the browser.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected Error",
                "The request could not be completed."
        );
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        return problem;
    }
}
