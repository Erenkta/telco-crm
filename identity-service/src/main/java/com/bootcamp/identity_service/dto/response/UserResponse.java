package com.bootcamp.identity_service.dto.response;

import com.bootcamp.identity_service.entity.enums.StatusEnum;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record UserResponse(
        UUID id,
        UUID customerId,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        StatusEnum status,
        String keycloakUserId,
        List<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
){
}
