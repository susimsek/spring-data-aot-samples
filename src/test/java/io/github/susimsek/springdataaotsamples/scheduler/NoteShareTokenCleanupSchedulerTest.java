package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.NoteShareToken;
import io.github.susimsek.springdataaotsamples.repository.NoteShareTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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

        NoteShareToken t1 = new NoteShareToken();
        t1.setId(1L);
        t1.setTokenHash("a");
        NoteShareToken t2 = new NoteShareToken();
        t2.setId(2L);
        t2.setTokenHash("b");
        when(noteShareTokenRepository.findExpiredOrRevoked(any())).thenReturn(List.of(t1, t2));

        scheduler.purgeExpiredAndRevoked();

        final Instant after = Instant.now();
        final Instant upperBound = after.plusSeconds(1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> idsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<String> cacheNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Iterable> tokenHashesCaptor = ArgumentCaptor.forClass(Iterable.class);
        var inOrder = inOrder(noteShareTokenRepository, cacheProvider);

        inOrder.verify(noteShareTokenRepository).findExpiredOrRevoked(instantCaptor.capture());
        inOrder.verify(noteShareTokenRepository).deleteAllByIdInBatch(idsCaptor.capture());
        inOrder.verify(cacheProvider)
                .clearCache(cacheNameCaptor.capture(), tokenHashesCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(cacheNameCaptor.getValue())
                .isEqualTo(NoteShareTokenRepository.NOTE_SHARE_TOKEN_BY_HASH_CACHE);

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull().isBetween(lowerBound, upperBound);
    }
}
