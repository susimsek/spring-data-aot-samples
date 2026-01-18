package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Query(
            """
            update RefreshToken t
            set t.revoked = true
            where t.userId = :userId and t.revoked = false
            """)
    int revokeAllByUserId(Long userId);

    long deleteByExpiresAtBefore(Instant cutoff);

    long deleteByRevokedTrue();
}
