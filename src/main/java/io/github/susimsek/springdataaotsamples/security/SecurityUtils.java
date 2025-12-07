package io.github.susimsek.springdataaotsamples.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@UtilityClass
public class SecurityUtils {
    public static final String AUTHORITIES_CLAIM = "auth";
    public static final String USER_ID_CLAIM = "userId";
    public static final String AUTH_COOKIE = "AUTH-TOKEN";
    public static final String REFRESH_COOKIE = "REFRESH-TOKEN";
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    public Optional<String> getCurrentUserLogin() {
        var ctx = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(ctx.getAuthentication()));
    }

    public Optional<Long> getCurrentUserId() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
            .filter(authentication -> authentication.getPrincipal() instanceof ClaimAccessor)
            .map(authentication -> (ClaimAccessor) authentication.getPrincipal())
            .map(principal -> principal.getClaim(USER_ID_CLAIM));
    }

    private String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        var principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        } else if (principal instanceof String username) {
            return username;
        }
        return null;
    }
}
