package io.github.susimsek.springdataaotsamples.config.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    private final Caffeine caffeine = new Caffeine();

    @Getter
    @Setter
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
