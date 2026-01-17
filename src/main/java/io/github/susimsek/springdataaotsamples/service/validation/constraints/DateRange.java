package io.github.susimsek.springdataaotsamples.service.validation.constraints;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.susimsek.springdataaotsamples.service.validation.DateRangeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
public @interface DateRange {

    String message() default "{app.validation.dateRange.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long minSeconds() default 0L;

    long maxSeconds() default Long.MAX_VALUE;
}
