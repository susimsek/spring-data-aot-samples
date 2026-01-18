package io.github.susimsek.springdataaotsamples.scheduler;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NoteShareTokenCleanupScheduler {

    private final NoteShareTokenRepository noteShareTokenRepository;
    private final CacheProvider cacheProvider;

    @Scheduled(cron = "0 30 1 * * ?")
    @Transactional
    public void purgeExpiredAndRevoked() {
        Instant now = Instant.now();

        var tokens = noteShareTokenRepository.findExpiredOrRevoked(now);
        for (NoteShareToken token : tokens) {
            Long tokenId = token.getId();
            String tokenHash = token.getTokenHash();
            noteShareTokenRepository.deleteById(tokenId);
            cacheProvider.clearCache(NoteShareToken.class.getName(), tokenId);
            cacheProvider.clearCache(
                    NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE, tokenHash);
        }
    }
}
