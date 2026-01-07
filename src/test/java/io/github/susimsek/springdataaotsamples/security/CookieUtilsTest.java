package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

class CookieUtilsTest {

    @Test
    void getCookieValueShouldReturnCookieValueWhenPresentAndNonBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SecurityUtils.AUTH_COOKIE, "token123"));

        String result = CookieUtils.getCookieValue(request, SecurityUtils.AUTH_COOKIE);

        assertThat(result).isEqualTo("token123");
    }

    @Test
    void getCookieValueShouldReturnNullWhenMissingOrBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SecurityUtils.AUTH_COOKIE, " "));

        assertThat(CookieUtils.getCookieValue(request, SecurityUtils.AUTH_COOKIE)).isNull();
        assertThat(CookieUtils.getCookieValue(request, SecurityUtils.REFRESH_COOKIE)).isNull();
    }

    @Test
    void authCookieShouldBuildSecureStrictCookieWithPositiveMaxAge() {
        Instant expiresAt = Instant.now().plusSeconds(120);

        ResponseCookie cookie = CookieUtils.authCookie("jwt-token", expiresAt);

        assertThat(cookie.getName()).isEqualTo(SecurityUtils.AUTH_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("jwt-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge().getSeconds()).isBetween(118L, 120L);
    }

    @Test
    void clearAuthCookieShouldExpireCookieImmediately() {
        ResponseCookie cookie = CookieUtils.clearAuthCookie();

        assertThat(cookie.getName()).isEqualTo(SecurityUtils.AUTH_COOKIE);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void refreshCookieShouldUseRefreshTokenNameAndSameAttributes() {
        Instant expiresAt = Instant.now().plusSeconds(90);

        ResponseCookie cookie = CookieUtils.refreshCookie("refresh-token", expiresAt);

        assertThat(cookie.getName()).isEqualTo(SecurityUtils.REFRESH_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge().getSeconds()).isBetween(88L, 90L);
    }

    @Test
    void clearRefreshCookieShouldExpireCookieImmediately() {
        ResponseCookie cookie = CookieUtils.clearRefreshCookie();

        assertThat(cookie.getName()).isEqualTo(SecurityUtils.REFRESH_COOKIE);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
