package io.github.susimsek.springdataaotsamples.config.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.util.StringUtils;

import static io.github.susimsek.springdataaotsamples.config.Constants.DEFAULT_AUDITOR;

public class RevisionInfoListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        if (!(revisionEntity instanceof RevisionInfo revisionInfo)) {
            return;
        }
        var auditor = HttpHeaderAuditorAware.resolveCurrentAuditor()
            .filter(StringUtils::hasText)
            .orElse(DEFAULT_AUDITOR);
        revisionInfo.setUsername(auditor);
    }
}
