package io.github.susimsek.springdataaotsamples.service.validation;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.HexColor;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

public class HexColorValidator implements ConstraintValidator<HexColor, String> {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // optional field
        }
        return HEX_PATTERN.matcher(value.trim()).matches();
    }
}
