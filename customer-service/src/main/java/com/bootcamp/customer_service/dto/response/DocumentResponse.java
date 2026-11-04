package com.bootcamp.customer_service.dto.response;

import com.bootcamp.customer_service.entity.enums.DocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        DocumentType documentType,
        String fileName,
        String storageKey,
        String contentType,
        long sizeBytes,
        OffsetDateTime createdAt
) {
}
