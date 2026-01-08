package io.github.susimsek.springdataaotsamples.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.util.StringUtils;

/**
 * Resolves bearer token from Authorization header first, then from an auth cookie for browser GETs.
 */
public class CookieAwareBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    @Setter private boolean allowCookie = true;

    public CookieAwareBearerTokenResolver() {
        // Defaults: disallow query/form parameters; allow cookies.
        delegate.setAllowUriQueryParameter(false);
        delegate.setAllowFormEncodedBodyParameter(false);
    }

    public void setAllowUriQueryParameter(boolean allowUriQueryParameter) {
        delegate.setAllowUriQueryParameter(allowUriQueryParameter);
    }

    public void setAllowFormEncodedBodyParameter(boolean allowFormEncodedBodyParameter) {
        delegate.setAllowFormEncodedBodyParameter(allowFormEncodedBodyParameter);
    }

    @Override
    public @Nullable String resolve(HttpServletRequest request) {
        String headerToken = delegate.resolve(request);
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        if (!allowCookie) {
            return null;
        }
        var cookieValue = CookieUtils.getCookieValue(request, SecurityUtils.AUTH_COOKIE);
        return StringUtils.hasText(cookieValue) ? cookieValue : null;
    }
}
