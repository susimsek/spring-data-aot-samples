package io.github.susimsek.springdataaotsamples.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.util.UriComponentsBuilder;

/** Login entry point that appends the originally requested URL as a "redirect" query parameter. */
public class RedirectAwareAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

  private static final String REDIRECT_PARAM = "redirect";

  public RedirectAwareAuthenticationEntryPoint(String loginFormUrl) {
    super(loginFormUrl);
  }

  @Override
  protected String buildRedirectUrlToLoginPage(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) {
    String loginUrl = super.buildRedirectUrlToLoginPage(request, response, authException);
    String requestUri = request.getRequestURI();

    // Avoid redirect loops if the login page itself was requested
    if (requestUri != null && requestUri.startsWith(getLoginFormUrl())) {
      return loginUrl;
    }

    String query = request.getQueryString();
    String target = query == null ? requestUri : requestUri + "?" + query;

    return UriComponentsBuilder.fromUriString(loginUrl)
        .queryParam(REDIRECT_PARAM, target)
        .build()
        .toUriString();
  }
}
