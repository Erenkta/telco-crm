package com.bootcamp.customer_service.dto.response;

import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.entity.enums.CustomerType;
import com.bootcamp.customer_service.entity.enums.KycStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

// List item. Deliberately has no addresses/documents: the paged list never touches child collections,
// so there is no N+1 and no "collection fetch + pagination applied in memory" problem.
public record CustomerSummaryResponse(
        UUID id,
        CustomerType type,
        String displayName,
        String email,
        String phoneNumber,
        CustomerStatus status,
        KycStatus kycStatus,
        Long version,
        OffsetDateTime createdAt
) {
}
