package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private CacheProvider cacheProvider;

    @InjectMocks private RefreshTokenCleanupScheduler scheduler;

    @Test
    void purgeExpiredAndRevokedShouldDeleteAndClearCache() {
        final Instant before = Instant.now();
        final Instant lowerBound = before.minusSeconds(1);

        when(refreshTokenRepository.findIdsExpiredOrRevoked(any())).thenReturn(List.of(1L, 2L));

        scheduler.purgeExpiredAndRevoked();

        final Instant after = Instant.now();
        final Instant upperBound = after.plusSeconds(1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Long>> idsCaptor = ArgumentCaptor.forClass(Iterable.class);
        var inOrder = inOrder(refreshTokenRepository, cacheProvider);

        inOrder.verify(refreshTokenRepository).findIdsExpiredOrRevoked(instantCaptor.capture());
        inOrder.verify(refreshTokenRepository).deleteExpiredOrRevoked(instantCaptor.getValue());
        inOrder.verify(cacheProvider)
                .clearCache(eq(RefreshToken.class.getName()), idsCaptor.capture());

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull().isBetween(lowerBound, upperBound);

        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);

        verify(refreshTokenRepository).deleteExpiredOrRevoked(captured);
    }
}
