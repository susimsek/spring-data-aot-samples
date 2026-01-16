package io.github.susimsek.springdataaotsamples.service.validation;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.EnumValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Set<String> acceptedValues;
    private String allowedDisplay;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        acceptedValues =
                Arrays.stream(constraintAnnotation.enumClass().getEnumConstants())
                        .map(Enum::name)
                        .collect(Collectors.toSet());
        allowedDisplay = String.join(", ", acceptedValues);
    }

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = acceptedValues.contains(value);
        if (!valid) {
            context.disableDefaultConstraintViolation();

            HibernateConstraintValidatorContext hibernateContext =
                    context.unwrap(HibernateConstraintValidatorContext.class)
                            .addMessageParameter("allowedValues", allowedDisplay);

            hibernateContext.buildConstraintViolationWithTemplate(
                            hibernateContext.getDefaultConstraintMessageTemplate())
                    .addConstraintViolation();
        }

        return valid;
    }
}
