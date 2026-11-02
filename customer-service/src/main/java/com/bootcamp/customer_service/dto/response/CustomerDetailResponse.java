package com.bootcamp.customer_service.dto.response;

import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.entity.enums.CustomerType;
import com.bootcamp.customer_service.entity.enums.KycStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Single customer view. Type-specific fields of the other customer type are null and left out of the JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerDetailResponse(
        UUID id,
        CustomerType type,
        String email,
        String phoneNumber,
        CustomerStatus status,
        KycStatus kycStatus,
        String kycRejectionReason,
        OffsetDateTime kycStatusChangedAt,
        String firstName,
        String lastName,
        String nationalId,
        String companyName,
        String taxNumber,
        String taxOffice,
        List<AddressResponse> addresses,
        List<DocumentResponse> documents,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
