package com.bootcamp.customer_service.dto.response;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String title,
        String line,
        String district,
        String city,
        String postalCode,
        String country
) {
}
