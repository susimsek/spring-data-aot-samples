package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import io.github.susimsek.springdataaotsamples.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NoteShareTokenCleanupScheduler {

    private final NoteShareTokenRepository noteShareTokenRepository;
    private final CacheService cacheService;

    @Scheduled(cron = "0 30 1 * * ?")
    public void purgeExpiredAndRevoked() {
        noteShareTokenRepository.deleteByExpiresAtBefore(Instant.now());
        noteShareTokenRepository.deleteByRevokedTrue();
        cacheService.clearCache(NoteShareToken.class.getName());
    }
}
