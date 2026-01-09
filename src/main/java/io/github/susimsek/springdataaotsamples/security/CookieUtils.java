package io.github.susimsek.springdataaotsamples.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@UtilityClass
public class CookieUtils {

    private static final String SAME_SITE_STRICT = "Strict";

    public @Nullable String getCookieValue(HttpServletRequest request, String name) {
        var cookie = WebUtils.getCookie(request, name);
        String val = cookie != null ? cookie.getValue() : null;
        return StringUtils.hasText(val) ? val : null;
    }

    public ResponseCookie authCookie(String tokenValue, Instant expiresAt) {
        long maxAge = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        return ResponseCookie.from(SecurityUtils.AUTH_COOKIE, tokenValue)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(SAME_SITE_STRICT)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(SecurityUtils.AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(SAME_SITE_STRICT)
                .maxAge(0)
                .build();
    }

    public ResponseCookie refreshCookie(String tokenValue, Instant expiresAt) {
        long maxAge = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        return ResponseCookie.from(SecurityUtils.REFRESH_COOKIE, tokenValue)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(SAME_SITE_STRICT)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(SecurityUtils.REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(SAME_SITE_STRICT)
                .maxAge(0)
                .build();
    }
}
