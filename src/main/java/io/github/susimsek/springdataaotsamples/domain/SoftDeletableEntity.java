package io.github.susimsek.springdataaotsamples.domain;

import io.github.susimsek.springdataaotsamples.config.audit.SoftDeleteAuditingListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(SoftDeleteAuditingListener.class)
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deleted_date")
    private Instant deletedDate;
}
