package io.github.susimsek.springdataaotsamples.service.validation;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Username;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<Username, String> {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // optional field
        }
        return USERNAME_PATTERN.matcher(value).matches();
    }
}
