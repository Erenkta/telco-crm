package com.bootcamp.customer_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCorporateCustomerRequest(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email must be formatted properly")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,
        @NotBlank(message = "Phone number must not be empty")
        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phoneNumber,
        @NotBlank(message = "Company name must not be empty")
        @Size(max = 200, message = "Company name must be at most 200 characters")
        String companyName,
        @NotBlank(message = "Tax number must not be empty")
        @Pattern(regexp = "\\d{10}", message = "Tax number must be 10 digits")
        String taxNumber,
        @NotBlank(message = "Tax office must not be empty")
        @Size(max = 100, message = "Tax office must be at most 100 characters")
        String taxOffice
) implements CreateCustomerRequest {
}
