package io.github.susimsek.springdataaotsamples.config;

import java.time.Duration;
import java.util.List;

@SuppressWarnings({"java:S115", "java:S1214"})
public interface ApplicationDefaults {
    interface Cache {
        interface Caffeine {
            Duration ttl = Duration.ofHours(1);

            int initialCapacity = 500;

            long maximumSize = 1000L;
        }
    }

    interface Security {
        String contentSecurityPolicy = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'self'; base-uri 'self'";

        interface Jwt {
            Duration accessTokenTtl = Duration.ofMinutes(30);
            Duration refreshTokenTtl = Duration.ofDays(30);
            Duration refreshTokenTtlForRememberMe = Duration.ofDays(30);
            List<String> audience = List.of();
        }
    }
}
