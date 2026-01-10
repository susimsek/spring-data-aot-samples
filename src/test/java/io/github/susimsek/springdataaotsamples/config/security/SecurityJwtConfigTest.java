package io.github.susimsek.springdataaotsamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

class SecurityJwtConfigTest {

    private ApplicationProperties props;
    private SecurityJwtConfig config;

    @BeforeEach
    void setUp() {
        props = new ApplicationProperties();
        props.getSecurity().getJwt().setSecret("my-secret-key-1234567890");
        props.getSecurity().getJwt().setIssuer("http://issuer");
        props.getSecurity().getJwt().setAudience(List.of("aud"));
        props.getSecurity().getJwt().setAccessTokenTtl(Duration.ofMinutes(5));
        config = new SecurityJwtConfig(props);
    }

    @Test
    void jwtDecoderShouldBeConfiguredWithSecret() {
        JwtDecoder decoder = config.jwtDecoder();
        assertThat(decoder).isNotNull();
    }

    @Test
    void jwtEncoderShouldBeConfiguredWithSecret() {
        JwtEncoder encoder = config.jwtEncoder();
        assertThat(encoder).isNotNull();
    }

    @Test
    void bearerTokenResolverShouldBeCookieAware() {
        BearerTokenResolver resolver = config.bearerTokenResolver();
        assertThat(resolver)
                .isInstanceOf(
                        io.github.susimsek.springdataaotsamples.security
                                .CookieAwareBearerTokenResolver.class);
    }
}
