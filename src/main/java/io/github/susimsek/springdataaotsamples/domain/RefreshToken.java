package io.github.susimsek.springdataaotsamples.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class RefreshToken {

  @Id
  @SequenceGenerator(
      name = "refresh_token_seq",
      sequenceName = "refresh_token_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq")
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token", nullable = false, unique = true, length = 128)
  private String token;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "remember_me", nullable = false)
  private boolean rememberMe = false;

  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @Transient private String rawToken;
}
