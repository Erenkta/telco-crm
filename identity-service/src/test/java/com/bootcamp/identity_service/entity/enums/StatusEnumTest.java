package com.bootcamp.identity_service.entity.enums;

import com.bootcamp.identity_service.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusEnumTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "ACTIVE,   INACTIVE",
            "ACTIVE,   DELETED",
            "ACTIVE,   BLOCKED",
            "INACTIVE, ACTIVE",
            "INACTIVE, DELETED",
            "INACTIVE, BLOCKED",
            "BLOCKED,  ACTIVE",
            "DELETED,  ACTIVE"
    })
    void transitionTo_returnsTarget_whenTransitionIsAllowed(StatusEnum from, StatusEnum to) {
        assertThat(from.transitionTo(to)).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "DELETED, ACTIVE",
            "DELETED, INACTIVE",
            "DELETED, BLOCKED",
            "BLOCKED, INACTIVE",
            "BLOCKED, DELETED"
    })
    void transitionTo_throws_whenTransitionIsNotAllowed(StatusEnum from, StatusEnum to) {
        assertThatThrownBy(() -> from.transitionTo(to))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    @ParameterizedTest(name = "{0} -> {0} is a no-op")
    @EnumSource(StatusEnum.class)
    void transitionTo_returnsSameStatus_whenTargetIsCurrentStatus(StatusEnum status) {
        assertThat(status.transitionTo(status)).isEqualTo(status);
    }

    @Test
    void invalidTransition_mapsToConflict() {
        assertThatThrownBy(() -> StatusEnum.DELETED.transitionTo(StatusEnum.ACTIVE))
                .isInstanceOfSatisfying(InvalidStatusTransitionException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
    }
}
