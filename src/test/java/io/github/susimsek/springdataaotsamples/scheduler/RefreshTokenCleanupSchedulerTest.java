package io.github.susimsek.springdataaotsamples.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenCleanupScheduler scheduler;

    @Test
    void purgeExpiredAndRevokedShouldDeleteExpiredAndRevokedTokens() {
        final Instant before = Instant.now();
        final Instant lowerBound = before.minusSeconds(1);

        when(refreshTokenRepository.findExpiredOrRevokedIds(any())).thenReturn(Set.of(1L, 2L));

        scheduler.purgeExpiredAndRevoked();

        final Instant after = Instant.now();
        final Instant upperBound = after.plusSeconds(1);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Set<Long>> idsCaptor = ArgumentCaptor.forClass(Set.class);
        var inOrder = inOrder(refreshTokenRepository);

        inOrder.verify(refreshTokenRepository).findExpiredOrRevokedIds(instantCaptor.capture());
        inOrder.verify(refreshTokenRepository).deleteAllByIdInBatch(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);

        Instant captured = instantCaptor.getValue();
        assertThat(captured).isNotNull().isBetween(lowerBound, upperBound);
    }
}
