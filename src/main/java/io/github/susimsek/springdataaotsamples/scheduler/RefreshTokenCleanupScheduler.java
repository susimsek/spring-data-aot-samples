package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 1 * * ?")
    public void purgeExpiredAndRevoked() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        refreshTokenRepository.deleteByRevokedTrue();
    }
}
