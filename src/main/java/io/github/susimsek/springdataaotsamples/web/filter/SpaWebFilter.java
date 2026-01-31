package io.github.susimsek.springdataaotsamples.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class SpaWebFilter extends OncePerRequestFilter {
    private static final Pattern LOCALE_ROUTE_PATTERN =
            Pattern.compile("^/([a-zA-Z]{2}(?:-[a-zA-Z]{2})?)(?:/([^/\\\\.]+))?(?:/.*)?/?$");

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

        boolean isPageRoute = path.startsWith("/") && !path.contains(".");

        if (!isBackendRoute && !isNextAsset && isPageRoute) {
            Matcher matcher = LOCALE_ROUTE_PATTERN.matcher(path);
            if (matcher.matches()) {
                String locale = matcher.group(1);
                String page = matcher.group(2);
                if (!StringUtils.hasText(page)) {
                    request.getRequestDispatcher("/" + locale + "/index.html")
                            .forward(request, response);
                    return;
                }
                request.getRequestDispatcher("/" + locale + "/" + page + "/index.html")
                        .forward(request, response);
                return;
            }

            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
