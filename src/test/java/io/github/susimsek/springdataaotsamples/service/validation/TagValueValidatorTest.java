package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagValueValidatorTest {

    private final TagValueValidator validator = new TagValueValidator();

    @Test
    void isValidShouldReturnTrueForNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValidShouldTrimAndAcceptValidTags() {
        assertThat(validator.isValid("tag", null)).isTrue();
        assertThat(validator.isValid(" tag ", null)).isTrue();
        assertThat(validator.isValid("tag_1", null)).isTrue();
        assertThat(validator.isValid("tag-1", null)).isTrue();
    }

    @Test
    void isValidShouldRejectInvalidTags() {
        assertThat(validator.isValid("tag.name", null)).isFalse();
        assertThat(validator.isValid("tag name", null)).isFalse();
        assertThat(validator.isValid("tag@", null)).isFalse();
    }
}

