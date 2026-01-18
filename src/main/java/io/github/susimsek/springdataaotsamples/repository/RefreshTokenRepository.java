package io.github.susimsek.springdataaotsamples.repository;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import java.time.Instant;
import java.util.List;
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

    @Query("select t.id from RefreshToken t where t.expiresAt < :now or t.revoked = true")
    List<Long> findIdsExpiredOrRevoked(Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now or t.revoked = true")
    int deleteExpiredOrRevoked(Instant now);
}
