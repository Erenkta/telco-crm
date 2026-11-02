package com.bootcamp.customer_service.dto.request;

import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.entity.enums.KycStatus;

// Bound from query parameters: GET /customers?email=..&status=..&kycStatus=..  Every field is optional.
public record CustomerSearchFilter(
        String email,
        CustomerStatus status,
        KycStatus kycStatus
) {
}
