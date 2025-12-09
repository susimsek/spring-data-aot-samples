package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CacheService cacheService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void purgeExpiredAndRevoked() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        refreshTokenRepository.deleteByRevokedTrue();
        cacheService.clearCache(RefreshToken.class.getName());
    }
}
