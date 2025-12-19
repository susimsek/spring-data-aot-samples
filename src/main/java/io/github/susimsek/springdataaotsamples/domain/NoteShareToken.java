package io.github.susimsek.springdataaotsamples.domain;

import io.github.susimsek.springdataaotsamples.domain.enumeration.SharePermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "note_share_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NoteShareToken extends AuditableEntity {

  @Id
  @SequenceGenerator(
      name = "note_share_token_seq",
      sequenceName = "note_share_token_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "note_share_token_seq")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "note_id", nullable = false)
  private Note note;

  @Enumerated(EnumType.STRING)
  @Column(name = "permission", nullable = false, length = 10)
  private SharePermission permission;

  @Column(name = "token_hash", nullable = false, length = 128, unique = true)
  private String tokenHash;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "one_time", nullable = false)
  private boolean oneTime = false;

  @Column(name = "use_count", nullable = false)
  private int useCount = 0;

  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;
}
