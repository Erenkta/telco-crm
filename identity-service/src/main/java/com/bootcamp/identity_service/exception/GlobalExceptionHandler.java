package com.bootcamp.identity_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler  {

    @ExceptionHandler(CRMBaseException.class)
    public ProblemDetail handleCrmException(CRMBaseException ex, HttpServletRequest request) {
        ProblemDetail response = ProblemDetail.forStatusAndDetail(ex.getStatusCode(),ex.generateExceptionSummary());

        response.setTitle(ex.generateExceptionTitle());
        response.setInstance(URI.create(request.getRequestURI()));
        response.setProperty("timestamp", Instant.now());
        return response;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {

        Map<String, List<String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        LinkedHashMap::new,
                        Collectors.mapping(this::resolveMessage, Collectors.toList())
                ));

        List<String> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(this::resolveMessage)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "İstek gövdesinde geçersiz alanlar var.");
        problem.setTitle("Validation");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", fieldErrors);

        if (!globalErrors.isEmpty()) {
            problem.setProperty("globalErrors", globalErrors);
        }

        return problem;
    }

    private String resolveMessage(ObjectError error) {
        return Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid Value");
    }
}
