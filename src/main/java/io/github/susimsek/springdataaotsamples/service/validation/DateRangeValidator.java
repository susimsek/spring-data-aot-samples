package io.github.susimsek.springdataaotsamples.service.validation;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.DateRange;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;

public class DateRangeValidator implements ConstraintValidator<DateRange, Instant> {

    private long minSeconds;
    private long maxSeconds;

    @Override
    public void initialize(DateRange constraintAnnotation) {
        this.minSeconds = Math.max(0L, constraintAnnotation.minSeconds());
        this.maxSeconds =
                constraintAnnotation.maxSeconds() <= 0
                        ? Long.MAX_VALUE
                        : constraintAnnotation.maxSeconds();
    }

    @Override
    public boolean isValid(Instant value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Instant now = Instant.now();
        Instant minInstant = now.plusSeconds(minSeconds);
        Instant maxInstant = (maxSeconds == Long.MAX_VALUE) ? null : now.plusSeconds(maxSeconds);

        if (value.isBefore(minInstant)) {
            return false;
        }
        if (maxInstant != null && value.isAfter(maxInstant)) {
            return false;
        }
        return true;
    }
}
