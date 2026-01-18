package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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

        RefreshToken t1 = new RefreshToken();
        t1.setId(1L);
        RefreshToken t2 = new RefreshToken();
        t2.setId(2L);
        when(refreshTokenRepository.findExpiredOrRevoked(any())).thenReturn(List.of(t1, t2));

        scheduler.purgeExpiredAndRevoked();

        final Instant after = Instant.now();
        final Instant upperBound = after.plusSeconds(1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        var inOrder = inOrder(refreshTokenRepository, cacheProvider);

        inOrder.verify(refreshTokenRepository).findExpiredOrRevoked(instantCaptor.capture());
        inOrder.verify(refreshTokenRepository).deleteById(1L);
        inOrder.verify(cacheProvider).clearCache(RefreshToken.class.getName(), 1L);
        inOrder.verify(refreshTokenRepository).deleteById(2L);
        inOrder.verify(cacheProvider).clearCache(RefreshToken.class.getName(), 2L);

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull().isBetween(lowerBound, upperBound);
    }
}
