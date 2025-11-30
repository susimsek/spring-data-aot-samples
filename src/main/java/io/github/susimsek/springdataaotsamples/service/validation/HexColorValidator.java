package io.github.susimsek.springdataaotsamples.service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class HexColorValidator implements ConstraintValidator<HexColor, String> {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#?[0-9A-Fa-f]{6}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // optional field
        }
        return HEX_PATTERN.matcher(value.trim()).matches();
    }
}
