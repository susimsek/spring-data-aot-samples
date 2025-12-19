package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.domain.RefreshToken;
import io.github.susimsek.springdataaotsamples.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void revoke(RefreshToken token) {
    if (token.isRevoked()) {
      return;
    }
    token.setRevoked(true);
    refreshTokenRepository.saveAndFlush(token);
  }
}
