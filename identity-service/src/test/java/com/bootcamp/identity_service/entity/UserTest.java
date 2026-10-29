package com.bootcamp.identity_service.entity;

import com.bootcamp.identity_service.entity.enums.StatusEnum;
import com.bootcamp.identity_service.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @ParameterizedTest(name = "{0} user is modifiable")
    @EnumSource(value = StatusEnum.class, names = {"ACTIVE", "INACTIVE"})
    void isModifiable_returnsTrue_forActiveOrInactiveUser(StatusEnum status) {
        User user = User.builder().status(status).build();

        assertThat(user.isModifiable()).isTrue();
    }

    @ParameterizedTest(name = "{0} user is not modifiable")
    @EnumSource(value = StatusEnum.class, names = {"BLOCKED", "DELETED"})
    void isModifiable_returnsFalse_forBlockedOrDeletedUser(StatusEnum status) {
        User user = User.builder().status(status).build();

        assertThat(user.isModifiable()).isFalse();
    }

    @Test
    void newUser_isActiveByDefault() {
        User user = User.builder().build();

        assertThat(user.getStatus()).isEqualTo(StatusEnum.ACTIVE);
    }

    @Test
    void changeStatus_updatesStatus_whenTransitionIsAllowed() {
        // Arrange
        User user = User.builder().status(StatusEnum.ACTIVE).build();

        // Act
        user.changeStatus(StatusEnum.BLOCKED);

        // Assert
        assertThat(user.getStatus()).isEqualTo(StatusEnum.BLOCKED);
    }

    @Test
    void changeStatus_throwsAndKeepsStatus_whenTransitionIsNotAllowed() {
        User user = User.builder().status(StatusEnum.DELETED).build();

        assertThatThrownBy(() -> user.changeStatus(StatusEnum.ACTIVE))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(user.getStatus()).isEqualTo(StatusEnum.DELETED);
    }
}
