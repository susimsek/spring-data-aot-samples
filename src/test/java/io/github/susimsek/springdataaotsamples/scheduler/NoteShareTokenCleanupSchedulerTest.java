package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import java.time.Instant;
import java.util.List;
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

        when(noteShareTokenRepository.findIdsExpiredOrRevoked(any())).thenReturn(List.of(1L, 2L));
        when(noteShareTokenRepository.findTokenHashesExpiredOrRevoked(any()))
                .thenReturn(List.of("a", "b"));

        scheduler.purgeExpiredAndRevoked();

        final Instant after = Instant.now();
        final Instant upperBound = after.plusSeconds(1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Long>> idsCaptor = ArgumentCaptor.forClass(Iterable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<String>> hashesCaptor = ArgumentCaptor.forClass(Iterable.class);
        var inOrder = inOrder(noteShareTokenRepository, cacheProvider);

        inOrder.verify(noteShareTokenRepository).findIdsExpiredOrRevoked(instantCaptor.capture());
        inOrder.verify(noteShareTokenRepository)
                .findTokenHashesExpiredOrRevoked(instantCaptor.getValue());
        inOrder.verify(noteShareTokenRepository).deleteExpiredOrRevoked(instantCaptor.getValue());
        inOrder.verify(cacheProvider)
                .clearCache(eq(NoteShareToken.class.getName()), idsCaptor.capture());
        inOrder.verify(cacheProvider)
                .clearCache(
                        eq(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE),
                        hashesCaptor.capture());

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull().isBetween(lowerBound, upperBound);

        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(hashesCaptor.getValue()).containsExactlyInAnyOrder("a", "b");

        verify(noteShareTokenRepository).deleteExpiredOrRevoked(captured);
    }
}
