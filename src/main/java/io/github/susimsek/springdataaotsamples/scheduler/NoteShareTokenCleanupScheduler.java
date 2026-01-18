package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteShareTokenCleanupScheduler {

    private final NoteShareTokenRepository noteShareTokenRepository;
    private final CacheProvider cacheProvider;

    @Scheduled(cron = "0 30 1 * * ?")
    public void purgeExpiredAndRevoked() {
        Instant now = Instant.now();

        var idsToEvict = noteShareTokenRepository.findIdsExpiredOrRevoked(now);
        var hashesToEvict = noteShareTokenRepository.findTokenHashesExpiredOrRevoked(now);
        noteShareTokenRepository.deleteExpiredOrRevoked(now);

        cacheProvider.clearCache(NoteShareToken.class.getName(), idsToEvict);
        cacheProvider.clearCache(
                NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE, hashesToEvict);
    }
}
