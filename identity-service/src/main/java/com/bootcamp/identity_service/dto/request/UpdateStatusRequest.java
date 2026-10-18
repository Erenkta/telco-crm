package com.bootcamp.identity_service.dto.request;

import com.bootcamp.identity_service.entity.enums.StatusEnum;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull
        StatusEnum status
) {
}
