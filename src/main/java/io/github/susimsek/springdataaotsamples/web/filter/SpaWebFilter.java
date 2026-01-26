package io.github.susimsek.springdataaotsamples.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class SpaWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean isBackendRoute =
                path.startsWith("/api")
                        || path.startsWith("/actuator")
                        || path.startsWith("/v3/api-docs")
                        || path.startsWith("/h2-console")
                        || path.startsWith("/swagger-ui");

        boolean isNextAsset = path.startsWith("/_next");

        boolean isPageRoute = path.matches("^/(?!.*\\.).*$");

        if (!isBackendRoute && !isNextAsset && isPageRoute) {
            String page = path.substring(1);
            String htmlPage = page.isEmpty() ? "/index.html" : "/" + page + ".html";
            request.getRequestDispatcher(htmlPage).forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
