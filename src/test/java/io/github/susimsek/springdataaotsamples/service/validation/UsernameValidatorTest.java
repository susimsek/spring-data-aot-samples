package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsernameValidatorTest {

    private final UsernameValidator validator = new UsernameValidator();

    @Test
    void isValidShouldReturnTrueForNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValidShouldAcceptValidUsernames() {
        assertThat(validator.isValid("alice", null)).isTrue();
        assertThat(validator.isValid("alice_1", null)).isTrue();
        assertThat(validator.isValid("alice.1", null)).isTrue();
        assertThat(validator.isValid("alice-1", null)).isTrue();
    }

    @Test
    void isValidShouldRejectInvalidUsernames() {
        assertThat(validator.isValid(" alice ", null)).isFalse();
        assertThat(validator.isValid("alice@", null)).isFalse();
        assertThat(validator.isValid("alice name", null)).isFalse();
    }
}

