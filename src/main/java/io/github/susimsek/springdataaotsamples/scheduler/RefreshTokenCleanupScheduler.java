package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void purgeExpiredAndRevoked() {
        Instant now = Instant.now();
        var ids = refreshTokenRepository.findExpiredOrRevokedIds(now);
        if (ids.isEmpty()) {
            return;
        }
        refreshTokenRepository.deleteAllByIdInBatch(ids);
    }
}
