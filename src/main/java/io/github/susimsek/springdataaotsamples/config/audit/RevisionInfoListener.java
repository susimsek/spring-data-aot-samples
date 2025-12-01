package io.github.susimsek.springdataaotsamples.config.audit;

import lombok.RequiredArgsConstructor;
import org.hibernate.envers.RevisionListener;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import static io.github.susimsek.springdataaotsamples.config.Constants.DEFAULT_AUDITOR;

@Component
@RequiredArgsConstructor
public class RevisionInfoListener implements RevisionListener {

    private final AuditorAware<String> auditorAware;

    @Override
    public void newRevision(Object revisionEntity) {
        if (!(revisionEntity instanceof RevisionInfo revisionInfo)) {
            return;
        }
        var auditor = auditorAware.getCurrentAuditor()
                .orElse(DEFAULT_AUDITOR);
        revisionInfo.setUsername(auditor);
    }
}
