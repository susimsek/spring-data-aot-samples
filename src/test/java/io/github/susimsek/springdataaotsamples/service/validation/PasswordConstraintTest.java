package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Password;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class PasswordConstraintTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptNull() {
        assertThat(validator.validate(new Sample(null))).isEmpty();
    }

    @Test
    void shouldAcceptValidPasswords() {
        assertThat(validator.validate(new Sample("Abcdefg1"))).isEmpty();
        assertThat(validator.validate(new Sample("Şifreğ1A"))).isEmpty();
    }

    @Test
    void shouldRejectPasswordsWithoutUppercaseLowercaseOrDigit() {
        assertThat(validator.validate(new Sample("abcdefg1"))).isNotEmpty();
        assertThat(validator.validate(new Sample("ABCDEFG1"))).isNotEmpty();
        assertThat(validator.validate(new Sample("Abcdefgh"))).isNotEmpty();
    }

    private record Sample(@Password String password) {}
}
