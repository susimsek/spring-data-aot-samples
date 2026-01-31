package io.github.susimsek.springdataaotsamples.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SpaWebFilterTest {

    private final SpaWebFilter filter = new SpaWebFilter();

    @Test
    void forwardsSpaRoutesToIndex() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo("/index.html");
    }

    @Test
    void forwardsLocaleRoutesToLocaleIndex() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/en");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo("/en/index.html");
    }

    @Test
    void forwardsLocalePageRoutesToLocalePageIndex() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/en/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo("/en/login/index.html");
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

    @Test
    void forwardsUnknownRoutesToHtmlPage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dffgg");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo("/index.html");
    }

    @Test
    void forwardsNonApiRoutesForNonGetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getForwardedUrl()).isEqualTo("/index.html");
    }
}
