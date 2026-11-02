package com.bootcamp.customer_service.entity.enums;

import com.bootcamp.common.exception.InvalidStatusTransitionException;

import java.util.EnumSet;
import java.util.Set;

public enum CustomerStatus {
    ACTIVE,
    SUSPENDED;

    public Set<CustomerStatus> allowedTransitions() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(SUSPENDED);
            case SUSPENDED -> EnumSet.of(ACTIVE);
        };
    }

    public CustomerStatus transitionTo(CustomerStatus target) {
        if (!allowedTransitions().contains(target)) {
            String message = String.format("Customer status cannot transition from %s to %s", this, target);
            throw new InvalidStatusTransitionException(message);
        }
        return target;
    }
}
