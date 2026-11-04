package com.bootcamp.customer_service.dto.request;

import com.bootcamp.customer_service.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Metadata of a file that was already uploaded to storage; this service never receives the file bytes.
public record AddDocumentRequest(
        @NotNull(message = "Document type must not be empty")
        DocumentType documentType,
        @NotBlank(message = "File name must not be empty")
        @Size(max = 255, message = "File name must be at most 255 characters")
        String fileName,
        @NotBlank(message = "Storage key must not be empty")
        @Size(max = 500, message = "Storage key must be at most 500 characters")
        String storageKey,
        @NotBlank(message = "Content type must not be empty")
        @Size(max = 100, message = "Content type must be at most 100 characters")
        String contentType,
        @Positive(message = "Size must be positive")
        long sizeBytes
) {
}
