package com.bootcamp.customer_service.repository;

import com.bootcamp.customer_service.dto.request.CustomerSearchFilter;
import com.bootcamp.customer_service.entity.Customer;
import com.bootcamp.customer_service.entity.enums.CustomerStatus;
import com.bootcamp.customer_service.entity.enums.KycStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Optional filters as composable pieces: only the filters the client sent are added to the WHERE clause.
// With derived queries every combination would need its own method (email, status, kycStatus -> 2^3 = 8 methods).
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> matching(CustomerSearchFilter filter) {
        List<Specification<Customer>> specs = new ArrayList<>();
        if (filter.email() != null && !filter.email().isBlank()) {
            specs.add(emailEquals(filter.email()));
        }
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status()));
        }
        if (filter.kycStatus() != null) {
            specs.add(hasKycStatus(filter.kycStatus()));
        }
        return Specification.allOf(specs);
    }

    // Emails are stored lower-case, so lower-casing only the input keeps the plain unique index usable.
    static Specification<Customer> emailEquals(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(root.get("email"), normalized);
    }

    static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Customer> hasKycStatus(KycStatus kycStatus) {
        return (root, query, cb) -> cb.equal(root.get("kycStatus"), kycStatus);
    }
}
