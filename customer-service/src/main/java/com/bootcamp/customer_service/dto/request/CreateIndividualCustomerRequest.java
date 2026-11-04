package com.bootcamp.customer_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateIndividualCustomerRequest(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email must be formatted properly")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,
        @NotBlank(message = "Phone number must not be empty")
        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phoneNumber,
        @NotBlank(message = "First name must not be empty")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,
        @NotBlank(message = "Last name must not be empty")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,
        @NotBlank(message = "National id must not be empty")
        @Pattern(regexp = "\\d{11}", message = "National id must be 11 digits")
        String nationalId
) implements CreateCustomerRequest {
}
