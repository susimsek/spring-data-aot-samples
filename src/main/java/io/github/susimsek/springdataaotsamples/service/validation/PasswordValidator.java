package io.github.susimsek.springdataaotsamples.service.validation;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.Password;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

public class PasswordValidator implements ConstraintValidator<Password, String> {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[a-zçğıöşüı])(?=.*[A-ZÇĞİÖŞÜ])(?=.*\\d)(?=.*[^A-Za-z0-9ÇĞİÖŞÜçğıöşüı])(?!.*\\s).+$");

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // optional field
        }
        return PASSWORD_PATTERN.matcher(value).matches();
    }
}
