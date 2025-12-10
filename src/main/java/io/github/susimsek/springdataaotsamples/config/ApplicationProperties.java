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
        private String contentSecurityPolicy = ApplicationDefaults.Security.contentSecurityPolicy;

        private final Jwt jwt = new Jwt();
    }

    @Data
    public static class Jwt {
        private String secret;
        private String issuer;
        private List<String> audience = ApplicationDefaults.Security.Jwt.audience;
        private Duration accessTokenTtl = ApplicationDefaults.Security.Jwt.accessTokenTtl;
        private Duration refreshTokenTtl = ApplicationDefaults.Security.Jwt.refreshTokenTtl;
        private Duration refreshTokenTtlForRememberMe = ApplicationDefaults.Security.Jwt.refreshTokenTtlForRememberMe;
    }

    @Data
    public static class Cache {
        private final Caffeine caffeine = new Caffeine();
    }

    @Data
    public static class Caffeine {
        private Duration ttl = ApplicationDefaults.Cache.Caffeine.ttl;
        private int initialCapacity = ApplicationDefaults.Cache.Caffeine.initialCapacity;
        private long maximumSize = ApplicationDefaults.Cache.Caffeine.maximumSize;
    }
}
