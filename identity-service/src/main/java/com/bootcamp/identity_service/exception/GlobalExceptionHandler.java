package com.bootcamp.identity_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String UNIQUE_VIOLATION = "23505";

    // Postgres'in varsayılan unique constraint isimleri (V1__init.sql)
    private static final Map<String, String> UNIQUE_CONSTRAINT_MESSAGES = Map.of(
            "users_email_key", "Email is already in use",
            "users_username_key", "Username is already in use",
            "roles_name_key", "Role name is already in use",
            "permissions_name_key", "Permission name is already in use",
            "uk_user_role", "Role is already assigned to the user"
    );

    @ExceptionHandler(CRMBaseException.class)
    public ProblemDetail handleCrmException(CRMBaseException ex, HttpServletRequest request) {
        ProblemDetail response = ProblemDetail.forStatusAndDetail(ex.getStatusCode(),ex.generateExceptionSummary());

        response.setTitle(ex.generateExceptionTitle());
        response.setInstance(URI.create(request.getRequestURI()));
        response.setProperty("timestamp", Instant.now());
        response.setProperty("trace","");
        return response;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(ObjectOptimisticLockingFailureException ex,HttpServletRequest request){
        log.warn(ex.getMessage());
        ProblemDetail response = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The record was modified by another request. Please reload and try again.");
        response.setTitle("Conflict");
        response.setInstance(URI.create(request.getRequestURI()));
        response.setProperty("timestamp", Instant.now());
        response.setProperty("trace","");
        return response;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        if (ex.getMostSpecificCause() instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
            String detail = UNIQUE_CONSTRAINT_MESSAGES.entrySet().stream()
                    .filter(e -> sql.getMessage().contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse("Record already exists");

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
            problem.setTitle("Conflict");
            problem.setInstance(URI.create(request.getRequestURI()));
            problem.setProperty("timestamp", Instant.now());
            return problem;
        }
        // NOT NULL, FK vb. ihlaller client'ın değil bizim hatamız
        return handleUnexpected(ex, request);
    }

    // Son çare: hiçbir handler'a uymayan hata. Detay sadece log'a gider, client'a gitmez.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // Framework exception'larının (400/404/405/415...) hepsi buradan geçer
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty("timestamp", Instant.now());
        }
        return response;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        LinkedHashMap::new,
                        Collectors.mapping(this::resolveMessage, Collectors.toList())
                ));

        List<String> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(this::resolveMessage)
                .toList();

        ProblemDetail problem = ex.getBody();
        problem.setTitle("Validation Error");
        problem.setDetail("Request body contains invalid fields.");
        problem.setProperty("errors", fieldErrors);
        if (!globalErrors.isEmpty()) {
            problem.setProperty("globalErrors", globalErrors);
        }

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private String resolveMessage(ObjectError error) {
        return Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid Value");
    }
}
