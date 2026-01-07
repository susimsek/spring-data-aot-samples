package io.github.susimsek.springdataaotsamples.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.audit.SecurityAuditorAware;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

class DatabaseConfigTest {

    private final DatabaseConfig config = new DatabaseConfig();

    @Test
    void auditorAwareShouldReturnSecurityAuditor() {
        AuditorAware<String> auditorAware = config.auditorAware();
        assertThat(auditorAware).isInstanceOf(SecurityAuditorAware.class);
    }
}
