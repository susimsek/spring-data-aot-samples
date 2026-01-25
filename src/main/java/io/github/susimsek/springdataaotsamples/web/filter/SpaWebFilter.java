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
        // Request URI includes the contextPath if any, remove it.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/api")
                && !path.startsWith("/actuator")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/h2-console")
                && !path.startsWith("/swagger-ui")
                && !path.contains(".")
                && path.matches("/(.*)")) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
