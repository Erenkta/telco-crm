package com.bootcamp.customer_service.controller;

import com.bootcamp.common.exception.PreconditionFailedException;
import com.bootcamp.common.exception.PreconditionRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Same package as IfMatch because the class is package-private.
class IfMatchTest {

    @ParameterizedTest(name = "If-Match {0} -> version {1}")
    @CsvSource(delimiter = '|', value = {
            "\"3\"    | 3",
            "W/\"3\"  | 3",
            "  \"12\" | 12",
            "7        | 7"
    })
    void requireVersion_parsesStrongAndWeakETags(String header, long expected) {
        assertThat(IfMatch.requireVersion(header)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requireVersion_throwsPreconditionRequired_whenHeaderIsMissing(String header) {
        assertThatThrownBy(() -> IfMatch.requireVersion(header))
                .isInstanceOf(PreconditionRequiredException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"abc\"", "*", "\"1\", \"2\""})
    void requireVersion_throwsPreconditionFailed_whenHeaderIsNotAVersion(String header) {
        assertThatThrownBy(() -> IfMatch.requireVersion(header))
                .isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void etagOf_quotesTheVersion() {
        assertThat(IfMatch.etagOf(4L)).isEqualTo("\"4\"");
    }
}
