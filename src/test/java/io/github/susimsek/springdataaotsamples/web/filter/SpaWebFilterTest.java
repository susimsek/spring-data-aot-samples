package io.github.susimsek.springdataaotsamples.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SpaWebFilterTest {

    private final SpaWebFilter filter = new SpaWebFilter();

    static Stream<Arguments> forwardingRoutes() {
        return Stream.of(
                Arguments.of("GET", "/login", "/index.html"),
                Arguments.of("GET", "/en", "/en/index.html"),
                Arguments.of("GET", "/en/login", "/en/login/index.html"),
                Arguments.of("GET", "/dffgg", "/index.html"),
                Arguments.of("POST", "/login", "/index.html"));
    }

    @ParameterizedTest
    @MethodSource("forwardingRoutes")
    void forwardsRoutes(String method, String path, String expectedForwardedUrl) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo(expectedForwardedUrl);
    }

    @Test
    void skipsApiRoutes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getForwardedUrl()).isNull();
    }

    @Test
    void skipsAssetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isNull();
    }
}
