package io.github.susimsek.springdataaotsamples.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
import io.github.susimsek.springdataaotsamples.web.filter.SpaWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApplicationProperties applicationProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        AccessDeniedHandlerImpl htmlAccessDenied = new AccessDeniedHandlerImpl();
        htmlAccessDenied.setErrorPage("/403");

        http.csrf(AbstractHttpConfigurer::disable)
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                applicationProperties
                                                                        .getSecurity()
                                                                        .getContentSecurityPolicy()))
                                        .frameOptions(
                                                HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                                        .referrerPolicy(
                                                ref ->
                                                        ref.policy(
                                                                ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                        .permissionsPolicyHeader(
                                                permissions ->
                                                        permissions.policy(
                                                                "camera=(), fullscreen=(self),"
                                                                        + " geolocation=(),"
                                                                        + " microphone=()"))
                                        .httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(31_536_000)))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex ->
                                ex.defaultAuthenticationEntryPointFor(
                                                new LoginUrlAuthenticationEntryPoint("/login"),
                                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                                        .defaultAccessDeniedHandlerFor(
                                                htmlAccessDenied,
                                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/auth/login", "/api/auth/refresh")
                                        .permitAll()
                                        .requestMatchers("/api/auth/register")
                                        .permitAll()
                                        .requestMatchers("/api/share/**")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasAuthority(AuthoritiesConstants.ADMIN)
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/actuator/info",
                                                "/actuator/prometheus")
                                        .permitAll()
                                        .requestMatchers("/actuator/**")
                                        .hasAuthority(AuthoritiesConstants.ADMIN)
                                        .requestMatchers(
                                                "/*.html",
                                                "/*.js",
                                                "/*.txt",
                                                "/*.json",
                                                "/*.map",
                                                "/*.css",
                                                "/_next/**")
                                        .permitAll()
                                        .requestMatchers("/*.ico", "/*.png", "/*.svg", "/*.webapp")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        // Forward unknown non-API routes to the Next.js SPA entrypoint (static export).
        http.addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
