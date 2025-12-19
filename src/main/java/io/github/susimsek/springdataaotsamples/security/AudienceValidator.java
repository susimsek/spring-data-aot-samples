package io.github.susimsek.springdataaotsamples.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;

/** Validates that a JWT contains at least one required audience value. */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR =
            new OAuth2Error("invalid_token", "The required audience is missing", null);

    private final List<String> requiredAudiences;

    public AudienceValidator(Collection<String> requiredAudiences) {
        this.requiredAudiences =
                requiredAudiences == null ? List.of() : List.copyOf(requiredAudiences);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (CollectionUtils.isEmpty(requiredAudiences)) {
            return OAuth2TokenValidatorResult.success();
        }
        var audiences = token.getAudience();
        boolean match =
                audiences != null && audiences.stream().anyMatch(requiredAudiences::contains);
        return match
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(ERROR);
    }
}
