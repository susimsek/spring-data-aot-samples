package io.github.susimsek.springdataaotsamples.security;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class CookieAwareBearerTokenResolverTest {

    private final CookieAwareBearerTokenResolver resolver = new CookieAwareBearerTokenResolver();

    @Test
    void resolveShouldPreferAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setCookies(new jakarta.servlet.http.Cookie(SecurityUtils.AUTH_COOKIE, "cookie-token"));

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo("header-token");
    }

    @Test
    void resolveShouldFallbackToCookieWhenAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<CookieUtils> cookies = mockStatic(CookieUtils.class)) {
            cookies.when(() -> CookieUtils.getCookieValue(request, SecurityUtils.AUTH_COOKIE))
                    .thenReturn("cookie-token");

            String token = resolver.resolve(request);

            assertThat(token).isEqualTo("cookie-token");
        }
    }

    @Test
    void resolveShouldReturnNullWhenNoToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void resolveShouldIgnoreCookieWhenDisabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        resolver.setAllowCookie(false);

        try (MockedStatic<CookieUtils> cookies = mockStatic(CookieUtils.class)) {
            cookies.when(() -> CookieUtils.getCookieValue(request, SecurityUtils.AUTH_COOKIE))
                    .thenReturn("cookie-token");

            String token = resolver.resolve(request);

            assertThat(token).isNull();
        }
    }

    @Test
    void setterShouldPropagateToDelegate() {
        // Ensure setters don't break resolution (header still preferred)
        resolver.setAllowUriQueryParameter(true);
        resolver.setAllowFormEncodedBodyParameter(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");
        request.setParameter("access_token", "query-token");

        assertThat(resolver.resolve(request)).isEqualTo("header-token");
    }
}
