package com.bootcamp.identity_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignRoleRequest(
        @NotEmpty(message = "Role ids must not be empty")
        List<@NotNull(message = "Role id must not be null") UUID> roles
) {
}
