package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RedirectAwareAuthenticationEntryPointTest {

    private final RedirectAwareAuthenticationEntryPoint entryPoint =
            new RedirectAwareAuthenticationEntryPoint("/login.html");

    @Test
    void shouldAppendRedirectParamForNonLoginRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes");
        request.setQueryString("q=test");

        String url =
                entryPoint.buildRedirectUrlToLoginPage(
                        request, new MockHttpServletResponse(), null);

        assertThat(url).isEqualTo("/login.html?redirect=/api/notes?q=test");
    }

    @Test
    void shouldAvoidRedirectLoopWhenAlreadyOnLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");

        String url =
                entryPoint.buildRedirectUrlToLoginPage(
                        request, new MockHttpServletResponse(), null);

        assertThat(url).isEqualTo("/login.html");
    }
}
