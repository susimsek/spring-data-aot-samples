package io.github.susimsek.springdataaotsamples.security;

import java.security.SecureRandom;
import java.util.HexFormat;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RandomUtils {

  public static final int REFRESH_TOKEN_BYTE_LENGTH = 64;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public String hexToken(int byteLength) {
    byte[] bytes = new byte[byteLength];
    SECURE_RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  public String hexRefreshToken() {
    return hexToken(REFRESH_TOKEN_BYTE_LENGTH);
  }
}
