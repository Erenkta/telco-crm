package com.bootcamp.customer_service.exception;

import com.bootcamp.common.exception.UniqueConstraintMessages;
import org.springframework.stereotype.Component;

import java.util.Map;

// Constraint names come from V1__init.sql. The common GlobalExceptionHandler turns a violation into a 409 with this message.
@Component
public class CustomerUniqueConstraintMessages implements UniqueConstraintMessages {
    @Override
    public Map<String, String> messages() {
        return Map.of(
                "uk_customers_email", "Email is already in use",
                "uk_customers_national_id", "A customer with this national id already exists",
                "uk_customers_tax_number", "A customer with this tax number already exists"
        );
    }
}
