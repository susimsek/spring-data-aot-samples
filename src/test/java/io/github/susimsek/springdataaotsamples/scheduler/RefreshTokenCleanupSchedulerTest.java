package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import java.time.Instant;
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
        Instant before = Instant.now();

        scheduler.purgeExpiredAndRevoked();

        Instant after = Instant.now();

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        var inOrder = inOrder(refreshTokenRepository, cacheProvider);

        inOrder.verify(refreshTokenRepository).deleteByExpiresAtBefore(instantCaptor.capture());
        inOrder.verify(refreshTokenRepository).deleteByRevokedTrue();
        inOrder.verify(cacheProvider).clearCache(RefreshToken.class.getName());

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured).isBetween(before.minusSeconds(1), after.plusSeconds(1));

        verify(refreshTokenRepository).deleteByExpiresAtBefore(captured);
        verify(refreshTokenRepository).deleteByRevokedTrue();
    }
}
