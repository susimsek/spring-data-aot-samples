package io.github.susimsek.springdataaotsamples.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * Secret used for HMAC signing. Replace in production.
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
}
