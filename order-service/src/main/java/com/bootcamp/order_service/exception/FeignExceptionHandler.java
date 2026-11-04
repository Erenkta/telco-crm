package com.bootcamp.order_service.exception;

import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FeignExceptionHandler{
    @ExceptionHandler(RetryableException.class)
    public ProblemDetail handleRetryException(RetryableException ex, HttpServletRequest request) {
        ProblemDetail response = ProblemDetail.forStatus(503);

        String title = "Client is not accessible.";
        response.setTitle(title);
        response.setDetail("Customer Service in temporarily unavailable.");
        response.setInstance(URI.create(request.getRequestURI()));
        response.setProperty("timestamp", Instant.now());
        response.setProperty("trace","");
        return response;
    }
}
