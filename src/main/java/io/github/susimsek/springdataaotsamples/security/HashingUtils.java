package io.github.susimsek.springdataaotsamples.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HashingUtils {

  public String sha256Hex(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash token", e);
    }
  }
}
