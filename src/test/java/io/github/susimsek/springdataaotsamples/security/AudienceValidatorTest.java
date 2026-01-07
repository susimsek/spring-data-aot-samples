package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    @Test
    void validateShouldSucceedWhenNoRequiredAudiencesConfigured() {
        AudienceValidator validator = new AudienceValidator(null);

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudiences(List.of()));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validateShouldSucceedWhenTokenContainsRequiredAudience() {
        AudienceValidator validator = new AudienceValidator(List.of("note-api"));

        OAuth2TokenValidatorResult result =
                validator.validate(jwtWithAudiences(List.of("note-api", "other")));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validateShouldFailWhenTokenDoesNotContainRequiredAudience() {
        AudienceValidator validator = new AudienceValidator(List.of("note-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudiences(List.of("other")));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .singleElement()
                .extracting(OAuth2Error::getErrorCode, OAuth2Error::getDescription)
                .containsExactly("invalid_token", "The required audience is missing");
    }

    private static Jwt jwtWithAudiences(Collection<String> audiences) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .audience(List.copyOf(audiences))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
