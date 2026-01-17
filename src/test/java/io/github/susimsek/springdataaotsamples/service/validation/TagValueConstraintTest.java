package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.TagValue;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class TagValueConstraintTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptNull() {
        assertThat(validator.validate(new Sample(null))).isEmpty();
    }

    @Test
    void shouldTrimAndAcceptValidTags() {
        assertThat(validator.validate(new Sample("tag"))).isEmpty();
        assertThat(validator.validate(new Sample(" tag "))).isEmpty();
        assertThat(validator.validate(new Sample("tag_1"))).isEmpty();
        assertThat(validator.validate(new Sample("tag-1"))).isEmpty();
    }

    @Test
    void shouldRejectInvalidTags() {
        assertThat(validator.validate(new Sample("tag.name"))).isNotEmpty();
        assertThat(validator.validate(new Sample("tag name"))).isNotEmpty();
        assertThat(validator.validate(new Sample("tag@"))).isNotEmpty();
    }

    private record Sample(@TagValue String tag) {}
}
