package io.github.susimsek.springdataaotsamples.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

  public static final String DEFAULT_AUDITOR = "system";
  public static final String AUDITOR_HEADER = "X-User";
}
