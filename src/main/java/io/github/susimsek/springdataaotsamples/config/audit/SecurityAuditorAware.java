package io.github.susimsek.springdataaotsamples.config.audit;

import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

import static io.github.susimsek.springdataaotsamples.config.Constants.DEFAULT_AUDITOR;

public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(SecurityUtils.getCurrentUserLogin().orElse(DEFAULT_AUDITOR));
    }
}
