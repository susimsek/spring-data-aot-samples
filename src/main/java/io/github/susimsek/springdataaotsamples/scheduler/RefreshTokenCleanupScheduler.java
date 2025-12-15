package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CacheProvider cacheProvider;

    @Scheduled(cron = "0 0 1 * * ?")
    public void purgeExpiredAndRevoked() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        refreshTokenRepository.deleteByRevokedTrue();
        cacheProvider.clearCache(RefreshToken.class.getName());
    }
}
