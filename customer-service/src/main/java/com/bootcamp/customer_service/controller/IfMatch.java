package com.bootcamp.customer_service.controller;

import com.bootcamp.common.exception.PreconditionFailedException;
import com.bootcamp.common.exception.PreconditionRequiredException;

// The customer's @Version is exposed as the ETag. Clients send it back in If-Match on updates.
// ETag format: "3" (quoted). A weak form W/"3" is also accepted when parsing.
final class IfMatch {

    private IfMatch() {
    }

    static String etagOf(Long version) {
        return "\"" + version + "\"";
    }

    static long requireVersion(String ifMatchHeader) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new PreconditionRequiredException(
                    "This endpoint requires an If-Match header with the customer's ETag (from GET /customers/{id}).");
        }
        String value = ifMatchHeader.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        value = value.replace("\"", "");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new PreconditionFailedException("If-Match header is not a valid customer ETag: " + ifMatchHeader);
        }
    }
}
