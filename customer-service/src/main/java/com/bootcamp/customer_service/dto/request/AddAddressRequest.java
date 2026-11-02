package com.bootcamp.customer_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddAddressRequest(
        @NotBlank(message = "Title must not be empty")
        @Size(max = 50, message = "Title must be at most 50 characters")
        String title,
        @NotBlank(message = "Address line must not be empty")
        @Size(max = 255, message = "Address line must be at most 255 characters")
        String line,
        @NotBlank(message = "District must not be empty")
        @Size(max = 100, message = "District must be at most 100 characters")
        String district,
        @NotBlank(message = "City must not be empty")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,
        @Size(max = 10, message = "Postal code must be at most 10 characters")
        String postalCode,
        // Optional, defaults to TR
        @Pattern(regexp = "[A-Z]{2}", message = "Country must be a 2-letter ISO code, e.g. TR")
        String country
) {
}
