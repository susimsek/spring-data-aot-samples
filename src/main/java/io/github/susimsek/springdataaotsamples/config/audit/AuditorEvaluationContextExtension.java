package io.github.susimsek.springdataaotsamples.config.audit;

import io.github.susimsek.springdataaotsamples.config.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.spel.spi.EvaluationContextExtension;

@RequiredArgsConstructor
public class AuditorEvaluationContextExtension implements EvaluationContextExtension {

    private final AuditorAware<String> auditorAware;

    @Override
    public String getExtensionId() {
        return "auditor";
    }

    @Override
    public Object getRootObject() {
        return new AuditorRoot(auditorAware.getCurrentAuditor().orElse(Constants.DEFAULT_AUDITOR));
    }
}
