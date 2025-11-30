package io.github.susimsek.springdataaotsamples.config.audit;

import io.github.susimsek.springdataaotsamples.domain.SoftDeletableEntity;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SoftDeleteAuditingListener {

    private final AuditorAware<String> auditorAware;

    @PreUpdate
    public void touchSoftDelete(Object target) {
        if (!(target instanceof SoftDeletableEntity entity)) {
            return;
        }
        if (!entity.isDeleted()) {
            return;
        }
        var auditor = auditorAware.getCurrentAuditor()
        .filter(StringUtils::hasText)
        .orElse(entity.getDeletedBy());
        entity.setDeletedBy(auditor);
        if (entity.getDeletedDate() == null) {
            entity.setDeletedDate(Instant.now());
        }
    }
}
