package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void isValidShouldReturnTrueForNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValidShouldAcceptStrongPasswords() {
        assertThat(validator.isValid("Abcdefg1!", null)).isTrue();
        assertThat(validator.isValid("ZyXwVuT9#", null)).isTrue();
        assertThat(validator.isValid("Şifreğ1!", null)).isTrue();
    }

    @Test
    void isValidShouldRejectWeakPasswords() {
        assertThat(validator.isValid("abcdefg1!", null)).isFalse(); // no uppercase
        assertThat(validator.isValid("ABCDEFG1!", null)).isFalse(); // no lowercase
        assertThat(validator.isValid("Abcdefghi!", null)).isFalse(); // no digit
        assertThat(validator.isValid("Abcdefghi1", null)).isFalse(); // no special
    }

    @Test
    void isValidShouldRejectPasswordsWithWhitespace() {
        assertThat(validator.isValid("Abcdefg1 !", null)).isFalse();
        assertThat(validator.isValid("Abcdefg1!\t", null)).isFalse();
    }
}
