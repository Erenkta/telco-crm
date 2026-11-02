package com.bootcamp.customer_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectKycRequest(
        @NotBlank(message = "Rejection reason must not be empty")
        @Size(max = 500, message = "Rejection reason must be at most 500 characters")
        String reason
) {
}
