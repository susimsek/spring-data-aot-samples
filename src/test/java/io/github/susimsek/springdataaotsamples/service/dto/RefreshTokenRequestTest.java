package io.github.susimsek.springdataaotsamples.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RefreshTokenRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldPassValidationWhenTokenWithinBounds() {
        RefreshTokenRequest request = new RefreshTokenRequest("x".repeat(30));

        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWhenTokenTooShort() {
        RefreshTokenRequest request = new RefreshTokenRequest("short");

        Set<ConstraintViolation<RefreshTokenRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("refreshToken"));
    }
}
