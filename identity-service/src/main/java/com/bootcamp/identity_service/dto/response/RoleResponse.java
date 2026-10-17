package com.bootcamp.identity_service.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record RoleResponse(
        UUID id,
        String name,
        String description,
        List<String> permissions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
