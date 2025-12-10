package io.github.susimsek.springdataaotsamples.config;

import java.time.Duration;
import java.util.List;

@SuppressWarnings({"java:S115", "java:S1214"})
public interface ApplicationDefaults {
    interface Cache {
        interface Caffeine {
            Duration timeToLiveDuration = Duration.ofHours(1);

            int initialCapacity = 500;

            long maximumSize = 1000L;
        }
    }

    interface Security {
        interface Jwt {
            Duration accessTokenTtl = Duration.ofMinutes(30);
            Duration refreshTokenTtl = Duration.ofDays(30);
            Duration refreshTokenTtlForRememberMe = Duration.ofDays(30);
            List<String> audience = List.of();
        }
    }
}
