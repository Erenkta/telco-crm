package com.bootcamp.customer_service.entity.enums;

import com.bootcamp.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CustomerStatusTest {


    @ParameterizedTest(name = "{0}->{1} is allowed")
    @CsvSource({
            "ACTIVE,    SUSPENDED",
            "SUSPENDED, ACTIVE"
    })
    void transitionTo_returnsTarget_whenTransitionIsAllowed(CustomerStatus from,CustomerStatus to){
        assertThat(from.transitionTo(to)).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0}->{1} is not allowed")
    @CsvSource({
            "ACTIVE,    ACTIVE",
            "SUSPENDED, SUSPENDED"
    })
    void transitionTo_throwsInvalidStatusTransition_whenTransitionIsNotAllowed(CustomerStatus from,CustomerStatus to){
        assertThatThrownBy(()->from.transitionTo(to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Customer status cannot transition");
    }
}
