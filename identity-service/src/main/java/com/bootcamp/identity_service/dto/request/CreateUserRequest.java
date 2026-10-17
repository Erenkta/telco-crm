package com.bootcamp.identity_service.dto.request;

import com.bootcamp.identity_service.entity.enums.StatusEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateUserRequest(
        UUID customerId,
        @NotBlank(message = "Username must not be empty")
        String username,
        @Email(message = "Email must be formatted properly")
        @NotBlank(message = "Email must not be empty")
        String email,
        @NotBlank(message = "Full name must not be empty")
        String fullName,
        @NotBlank(message = "Phone number must not be empty")
        String phoneNumber
) {
}
