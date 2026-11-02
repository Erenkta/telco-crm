package com.bootcamp.customer_service.entity.enums;


import com.bootcamp.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class KycStatusTest {

    @ParameterizedTest(name = "{0}->{1} is allowed")
    @CsvSource({
            "PENDING,   VERIFIED",
            "PENDING,   REJECTED",
            "VERIFIED,  REJECTED"
    })
    void transitionTo_returnsTarget_whenTransitionIsAllowed(KycStatus from,KycStatus to){
        assertThat(from.transitionTo(to)).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} -> {1} is not allowed")
    @CsvSource({
            "PENDING,   PENDING",
            "VERIFIED,  PENDING",
            "VERIFIED,  VERIFIED",
            "REJECTED,  REJECTED",
            "REJECTED,  VERIFIED",
            "REJECTED,  PENDING"
    })
    void transitionTo_throwsInvalidStatusTransition_whenTransitionIsNotAllowed(KycStatus from,KycStatus to){
        assertThatThrownBy(()->from.transitionTo(to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("KYC status cannot transition");
    }

}
