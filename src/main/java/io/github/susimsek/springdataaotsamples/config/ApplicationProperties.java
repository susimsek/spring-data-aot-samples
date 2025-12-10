package io.github.susimsek.springdataaotsamples.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Data
public class ApplicationProperties {

    private final Security security = new Security();
    private final Cache cache = new Cache();

    @Data
    public static class Security {
        private final Jwt jwt = new Jwt();
    }

    @Data
    public static class Jwt {
        /**
         * Secret used for HMAC signing.
         */
        private String secret;

        /**
         * Token issuer value.
         */
        private String issuer;

        /**
         * Expected audiences for issued tokens.
         */
        private List<String> audience = ApplicationDefaults.Security.Jwt.audience;

        /**
         * Access token lifetime.
         */
        private Duration accessTokenTtl = ApplicationDefaults.Security.Jwt.accessTokenTtl;

        /**
         * Refresh token lifetime.
         */
        private Duration refreshTokenTtl = ApplicationDefaults.Security.Jwt.refreshTokenTtl;

        /**
         * Refresh token lifetime when remember-me is enabled.
         */
        private Duration refreshTokenTtlForRememberMe = ApplicationDefaults.Security.Jwt.refreshTokenTtlForRememberMe;
    }

    @Data
    public static class Cache {
        private final Caffeine caffeine = new Caffeine();
    }

    @Data
    public static class Caffeine {
        /**
         * Time-to-live for cache entries.
         */
        private Duration ttl = Duration.ofHours(1);

        /**
         * Initial capacity for cache maps.
         */
        private int initialCapacity = 500;

        /**
         * Maximum cache size before evictions.
         */
        private long maximumSize = 1_000;
    }
}
