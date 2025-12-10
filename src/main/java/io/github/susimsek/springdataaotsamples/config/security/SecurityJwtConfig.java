package io.github.susimsek.springdataaotsamples.config.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.security.AudienceValidator;
import io.github.susimsek.springdataaotsamples.security.CookieAwareBearerTokenResolver;
import io.github.susimsek.springdataaotsamples.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static io.github.susimsek.springdataaotsamples.security.SecurityUtils.JWT_ALGORITHM;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityJwtConfig {

    private final ApplicationProperties applicationProperties;

    @Bean
    public JwtDecoder jwtDecoder() {
        var jwt = applicationProperties.getSecurity().getJwt();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(jwt))
                .macAlgorithm(JWT_ALGORITHM)
                .build();

        var audienceValidator = new AudienceValidator(jwt.getAudience());
        var withIssuer = JwtValidators.createDefaultWithIssuer(jwt.getIssuer());
        var withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        decoder.setJwtValidator(withAudience);
        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        var jwt = applicationProperties.getSecurity().getJwt();
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(jwt)));
    }

    private SecretKey secretKey(ApplicationProperties.Jwt jwt) {
        byte[] keyBytes = jwt.getSecret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(SecurityUtils.AUTHORITIES_CLAIM);
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        CookieAwareBearerTokenResolver resolver = new CookieAwareBearerTokenResolver();
        resolver.setAllowUriQueryParameter(false);
        resolver.setAllowFormEncodedBodyParameter(false);
        resolver.setAllowCookie(true);
        return resolver;
    }
}
