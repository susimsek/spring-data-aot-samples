package io.github.susimsek.springdataaotsamples.config.audit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SecurityAuditorAwareTest {

    @Test
    void shouldReturnCurrentUserOrDefault() {
        SecurityAuditorAware auditorAware = new SecurityAuditorAware();

        try (MockedStatic<SecurityUtils> utils = Mockito.mockStatic(SecurityUtils.class)) {
            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("alice"));
            assertThat(auditorAware.getCurrentAuditor()).contains("alice");

            utils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());
            assertThat(auditorAware.getCurrentAuditor()).contains("system");
        }
    }
}
