package com.bootcamp.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank(message = "Name must not be blank")
        String name,
        @NotBlank(message="Description must not be blank")
        String description
) {
}
