package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CacheProvider cacheProvider;

    @Scheduled(cron = "0 0 1 * * ?")
    public void purgeExpiredAndRevoked() {
        Instant now = Instant.now();
        var idsToEvict = refreshTokenRepository.findIdsExpiredOrRevoked(now);
        refreshTokenRepository.deleteExpiredOrRevoked(now);
        cacheProvider.clearCache(RefreshToken.class.getName(), idsToEvict);
    }
}
