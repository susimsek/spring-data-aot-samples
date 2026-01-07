package io.github.susimsek.springdataaotsamples.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    @Test
    void revokeShouldPersistWhenNotRevoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);

        refreshTokenService.revoke(token);

        verify(refreshTokenRepository).saveAndFlush(token);
    }

    @Test
    void revokeShouldSkipWhenAlreadyRevoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);

        refreshTokenService.revoke(token);

        verify(refreshTokenRepository, never()).saveAndFlush(token);
    }
}
