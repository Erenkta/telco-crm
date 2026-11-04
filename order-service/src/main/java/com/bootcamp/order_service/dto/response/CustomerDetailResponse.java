package com.bootcamp.order_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Single customer view. Type-specific fields of the other customer type are null and left out of the JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerDetailResponse(
        UUID id,
        String type,
        String email,
        String phoneNumber,
        String status,
        String kycStatus,
        String kycRejectionReason,
        OffsetDateTime kycStatusChangedAt,
        String firstName,
        String lastName,
        String nationalId,
        String companyName,
        String taxNumber,
        String taxOffice,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
