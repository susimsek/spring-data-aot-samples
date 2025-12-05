package io.github.susimsek.springdataaotsamples.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Secret used for HMAC signing.
     */
    private String secret;

    /**
     * Token issuer value.
     */
    private String issuer;

    /**
     * Access token lifetime.
     */
    private Duration accessTokenTtl = Duration.ofMinutes(30);

    /**
     * Refresh token lifetime.
     */
    private Duration refreshTokenTtl = Duration.ofDays(30);

    /**
     * Expected audiences for issued tokens.
     */
    private List<String> audience = List.of("note-api");
}
