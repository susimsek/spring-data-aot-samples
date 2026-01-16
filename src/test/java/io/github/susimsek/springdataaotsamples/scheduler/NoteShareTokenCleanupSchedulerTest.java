package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteShareTokenCleanupSchedulerTest {

    @Mock private NoteShareTokenRepository noteShareTokenRepository;

    @Mock private CacheProvider cacheProvider;

    @InjectMocks private NoteShareTokenCleanupScheduler scheduler;

    @Test
    void purgeExpiredAndRevokedShouldDeleteAndClearCaches() {
        final Instant before = Instant.now();
        final Instant lowerBound = before.minusSeconds(1);

        scheduler.purgeExpiredAndRevoked();

        final Instant after = Instant.now();
        final Instant upperBound = after.plusSeconds(1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        var inOrder = inOrder(noteShareTokenRepository, cacheProvider);

        inOrder.verify(noteShareTokenRepository).deleteByExpiresAtBefore(instantCaptor.capture());
        inOrder.verify(noteShareTokenRepository).deleteByRevokedTrue();
        inOrder.verify(cacheProvider)
                .clearCaches(
                        NoteShareToken.class.getName(),
                        NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE);

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull().isBetween(lowerBound, upperBound);

        verify(noteShareTokenRepository).deleteByExpiresAtBefore(captured);
        verify(noteShareTokenRepository).deleteByRevokedTrue();
    }
}
