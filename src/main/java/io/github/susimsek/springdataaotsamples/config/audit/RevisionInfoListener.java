package io.github.susimsek.springdataaotsamples.config.audit;

import lombok.Setter;
import org.hibernate.envers.RevisionListener;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import static io.github.susimsek.springdataaotsamples.config.Constants.DEFAULT_AUDITOR;

@Component
public class RevisionInfoListener implements RevisionListener {

    @Setter(onMethod_ = @Autowired)
    private @Nullable AuditorAware<String> auditorAware;

    @Override
    public void newRevision(Object revisionEntity) {
        if (!(revisionEntity instanceof RevisionInfo revisionInfo)) {
            return;
        }

        if (auditorAware != null) {
            var auditor = auditorAware.getCurrentAuditor()
                .orElse(DEFAULT_AUDITOR);
            revisionInfo.setUsername(auditor);
        }
    }
}
