package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HexColorValidatorTest {

    private final HexColorValidator validator = new HexColorValidator();

    @Test
    void isValidShouldReturnTrueForNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValidShouldAcceptValidHexWithOrWithoutHash() {
        assertThat(validator.isValid("#2563eb", null)).isTrue();
        assertThat(validator.isValid("2563eb", null)).isTrue();
        assertThat(validator.isValid("  #2563EB  ", null)).isTrue();
    }

    @Test
    void isValidShouldRejectInvalidHexValues() {
        assertThat(validator.isValid("#2563e", null)).isFalse();
        assertThat(validator.isValid("#2563eb00", null)).isFalse();
        assertThat(validator.isValid("#zzzzzz", null)).isFalse();
        assertThat(validator.isValid("#12 34 56", null)).isFalse();
    }
}
