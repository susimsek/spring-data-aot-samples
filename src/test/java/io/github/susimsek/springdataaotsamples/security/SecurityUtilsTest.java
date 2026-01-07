package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserLoginShouldReturnJwtSubject() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("jwt-user")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .claim(SecurityUtils.USER_ID_CLAIM, 10L)
                        .build();
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.getCurrentUserLogin()).contains("jwt-user");
    }

    @Test
    void getCurrentUserLoginShouldReturnUsernameWhenPrincipalIsString() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("plain-user", "pwd");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.getCurrentUserLogin()).contains("plain-user");
    }

    @Test
    void getCurrentUserIdShouldReturnUserIdClaimFromJwt() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("jwt-user")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .claim(SecurityUtils.USER_ID_CLAIM, 42L)
                        .build();
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.getCurrentUserId()).contains(42L);
    }

    @Test
    void hasCurrentUserAnyOfAuthoritiesShouldDetectPresence() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "pwd",
                        List.of(
                                new SimpleGrantedAuthority(AuthoritiesConstants.USER),
                                new SimpleGrantedAuthority("ROLE_AUDITOR")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER))
                .isTrue();
        assertThat(
                        SecurityUtils.hasCurrentUserAnyOfAuthorities(
                                AuthoritiesConstants.ADMIN, "ROLE_UNKNOWN"))
                .isFalse();
    }

    @Test
    void isCurrentUserAdminShouldCheckAdminRole() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "pwd",
                        List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.isCurrentUserAdmin()).isTrue();
    }

    @Test
    void emptyContextShouldReturnEmptyOptionals() {
        SecurityContextHolder.clearContext();

        assertThat(SecurityUtils.getCurrentUserLogin()).isEmpty();
        assertThat(SecurityUtils.getCurrentUserId()).isEmpty();
    }
}
