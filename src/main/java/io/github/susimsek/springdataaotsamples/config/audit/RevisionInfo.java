package io.github.susimsek.springdataaotsamples.config.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Getter
@Setter
@Entity
@Table(name = "revinfo")
@RevisionEntity(RevisionInfoListener.class)
public class RevisionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rev_seq")
    @SequenceGenerator(name = "rev_seq", sequenceName = "rev_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev", nullable = false, updatable = false)
    private Long id;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private Instant timestamp;

    @Column(name = "username", length = 100)
    private String username;
}
