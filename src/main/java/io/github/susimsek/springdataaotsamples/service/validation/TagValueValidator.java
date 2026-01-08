package io.github.susimsek.springdataaotsamples.service.validation;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.TagValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class TagValueValidator implements ConstraintValidator<TagValue, String> {

    private static final Pattern TAG_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // optional field
        }
        var trimmed = value.trim();
        return TAG_PATTERN.matcher(trimmed).matches();
    }
}
