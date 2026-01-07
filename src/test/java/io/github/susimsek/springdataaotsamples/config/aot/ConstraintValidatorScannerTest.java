package io.github.susimsek.springdataaotsamples.config.aot;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintValidator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConstraintValidatorScannerTest {

    @Test
    void scanShouldReturnValidatorClasses() {
        ConstraintValidatorScanner scanner =
                new ConstraintValidatorScanner(
                        "io.github.susimsek.springdataaotsamples.service.validation",
                        getClass().getClassLoader());

        Set<Class<?>> validators = scanner.scan();

        assertThat(validators).anyMatch(ConstraintValidator.class::isAssignableFrom);
    }

    @Test
    void scanShouldReturnEmptyForMissingPackage() {
        ConstraintValidatorScanner scanner =
                new ConstraintValidatorScanner("invalid.package", getClass().getClassLoader());

        Set<Class<?>> result = scanner.scan();

        assertThat(result).isEmpty();
    }
}
