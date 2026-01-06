package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.DateRange;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DateRangeValidatorTest {

    @Test
    void isValidShouldReturnTrueForNull() {
        DateRangeValidator validator = validator(0, 10);

        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValidShouldRejectValuesBeforeMinInstant() {
        DateRangeValidator validator = validator(10, Long.MAX_VALUE);
        Instant value = Instant.now().plusSeconds(5);

        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void isValidShouldRejectValuesAfterMaxInstant() {
        DateRangeValidator validator = validator(0, 5);
        Instant value = Instant.now().plusSeconds(100);

        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void isValidShouldAcceptValuesWithinRange() {
        DateRangeValidator validator = validator(0, 60);
        Instant value = Instant.now().plusSeconds(10);

        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    void initializeShouldClampMinSecondsToZero() {
        DateRangeValidator validator = validator(-5, 60);

        assertThat(validator.isValid(Instant.now().plusSeconds(1), null)).isTrue();
        assertThat(validator.isValid(Instant.now().minusSeconds(1), null)).isFalse();
    }

    @Test
    void initializeShouldDisableMaxWhenMaxSecondsIsZeroOrNegative() {
        DateRangeValidator validator = validator(0, 0);

        assertThat(validator.isValid(Instant.now().plusSeconds(10_000), null)).isTrue();
    }

    private static DateRangeValidator validator(long minSeconds, long maxSeconds) {
        DateRange annotation = mock(DateRange.class);
        when(annotation.minSeconds()).thenReturn(minSeconds);
        when(annotation.maxSeconds()).thenReturn(maxSeconds);

        DateRangeValidator validator = new DateRangeValidator();
        validator.initialize(annotation);
        return validator;
    }
}
