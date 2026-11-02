package com.bootcamp.customer_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Only contact info is updatable. Legal identity (name, national id, tax number) is fixed once KYC may have checked it.
public record UpdateContactRequest(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Email must be formatted properly")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,
        @NotBlank(message = "Phone number must not be empty")
        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phoneNumber
) {
}
