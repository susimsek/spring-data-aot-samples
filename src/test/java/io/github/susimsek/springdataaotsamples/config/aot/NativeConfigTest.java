package io.github.susimsek.springdataaotsamples.config.aot;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.web.error.Violation;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

class NativeConfigTest {

    @Test
    void registerHintsShouldRegisterResourcesAndReflections() {
        NativeConfig config = new NativeConfig();
        RuntimeHints hints = new RuntimeHints();

        config.registerHints(hints, getClass().getClassLoader());

        // check a couple of representative hints
        assertThat(hints.resources().resourcePatternHints().toList()).isNotEmpty();
        assertThat(hints.reflection().getTypeHint(TypeReference.of(Violation.class))).isNotNull();
    }
}
