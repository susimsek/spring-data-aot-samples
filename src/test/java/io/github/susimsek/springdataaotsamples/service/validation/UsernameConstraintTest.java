package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Username;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UsernameConstraintTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptNull() {
        assertThat(validator.validate(new Sample(null))).isEmpty();
    }

    @Test
    void shouldAcceptValidUsernames() {
        assertThat(validator.validate(new Sample("alice"))).isEmpty();
        assertThat(validator.validate(new Sample("ALICE_1"))).isEmpty();
        assertThat(validator.validate(new Sample("alice-1"))).isEmpty();
        assertThat(validator.validate(new Sample("a_b"))).isEmpty();
    }

    @Test
    void shouldRejectInvalidUsernames() {
        assertThat(validator.validate(new Sample(" alice "))).isNotEmpty();
        assertThat(validator.validate(new Sample("alice@"))).isNotEmpty();
        assertThat(validator.validate(new Sample("alice name"))).isNotEmpty();
        assertThat(validator.validate(new Sample("alice.1"))).isNotEmpty();
    }

    private record Sample(@Username String username) {}
}
