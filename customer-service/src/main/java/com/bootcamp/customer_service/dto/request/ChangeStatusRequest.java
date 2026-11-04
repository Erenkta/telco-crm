package com.bootcamp.customer_service.dto.request;

import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = "Status must not be empty")
        CustomerStatus status
) {
}
