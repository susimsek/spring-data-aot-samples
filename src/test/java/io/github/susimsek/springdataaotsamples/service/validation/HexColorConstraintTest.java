package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.HexColor;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class HexColorConstraintTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptNull() {
        assertThat(validator.validate(new Sample(null))).isEmpty();
    }

    @Test
    void shouldAcceptValidHexWithOrWithoutHash() {
        assertThat(validator.validate(new Sample("#2563eb"))).isEmpty();
        assertThat(validator.validate(new Sample("2563eb"))).isEmpty();
        assertThat(validator.validate(new Sample("  #2563EB  "))).isEmpty();
    }

    @Test
    void shouldRejectInvalidHexValues() {
        assertThat(validator.validate(new Sample("#2563e"))).isNotEmpty();
        assertThat(validator.validate(new Sample("#2563eb00"))).isNotEmpty();
        assertThat(validator.validate(new Sample("#zzzzzz"))).isNotEmpty();
        assertThat(validator.validate(new Sample("#12 34 56"))).isNotEmpty();
    }

    private record Sample(@HexColor String color) {}
}
