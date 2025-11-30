package io.github.susimsek.springdataaotsamples.service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Set<String> acceptedValues;
    private String allowedDisplay;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        acceptedValues = Arrays.stream(constraintAnnotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
        allowedDisplay = String.join(", ", acceptedValues);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = acceptedValues.contains(value);
        if (!valid) {
            context.disableDefaultConstraintViolation();

            HibernateConstraintValidatorContext hContext =
                context.unwrap(HibernateConstraintValidatorContext.class)
                    .addMessageParameter("allowedValues", allowedDisplay);

            hContext.buildConstraintViolationWithTemplate(hContext.getDefaultConstraintMessageTemplate())
                .addConstraintViolation();
        }

        return valid;
    }
}
