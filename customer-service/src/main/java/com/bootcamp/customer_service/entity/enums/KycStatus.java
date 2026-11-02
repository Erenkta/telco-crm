package com.bootcamp.customer_service.entity.enums;

import com.bootcamp.common.exception.InvalidStatusTransitionException;

import java.util.EnumSet;
import java.util.Set;

// All KYC transitions live here. VERIFIED -> REJECTED is "withdrawing an approval" (e.g. a forged document
// is detected later); in S6 it will trigger cancelling the customer's orders.
// REJECTED is final: a rejected customer cannot be verified again.
public enum KycStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    public Set<KycStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(VERIFIED, REJECTED);
            case VERIFIED -> EnumSet.of(REJECTED);
            case REJECTED -> EnumSet.noneOf(KycStatus.class);
        };
    }

    public KycStatus transitionTo(KycStatus target) {
        if (!allowedTransitions().contains(target)) {
            String message = String.format("KYC status cannot transition from %s to %s", this, target);
            throw new InvalidStatusTransitionException(message);
        }
        return target;
    }
}
