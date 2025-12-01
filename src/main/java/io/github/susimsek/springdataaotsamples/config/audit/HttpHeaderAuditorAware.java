package io.github.susimsek.springdataaotsamples.config.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static io.github.susimsek.springdataaotsamples.config.Constants.AUDITOR_HEADER;
import static io.github.susimsek.springdataaotsamples.config.Constants.DEFAULT_AUDITOR;

public class HttpHeaderAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return resolveCurrentAuditor();
    }

    private Optional<String> resolveCurrentAuditor() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes requestAttributes) {
            var header = requestAttributes.getRequest().getHeader(AUDITOR_HEADER);
            if (StringUtils.hasText(header)) {
                return Optional.of(header);
            }
        }
        return Optional.of(DEFAULT_AUDITOR);
    }
}
