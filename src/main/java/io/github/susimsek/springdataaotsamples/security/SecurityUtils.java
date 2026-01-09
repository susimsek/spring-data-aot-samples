package io.github.susimsek.springdataaotsamples.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
public class SecurityUtils {
    public static final String AUTHORITIES_CLAIM = "auth";
    public static final String USER_ID_CLAIM = "userId";
    public static final String AUTH_COOKIE = "AUTH-TOKEN";
    public static final String REFRESH_COOKIE = "REFRESH-TOKEN";
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    public Optional<String> getCurrentUserLogin() {
        var ctx = SecurityContextHolder.getContext();
        String principal = extractPrincipal(ctx.getAuthentication());
        return principal == null ? Optional.empty() : Optional.of(principal);
    }

    public Optional<Long> getCurrentUserId() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getPrincipal() instanceof ClaimAccessor)
                .map(authentication -> (ClaimAccessor) authentication.getPrincipal())
                .map(principal -> principal.getClaim(USER_ID_CLAIM));
    }

    public boolean hasCurrentUserThisAuthority(String authority) {
        return hasCurrentUserAnyOfAuthorities(authority);
    }

    public boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> required = Set.of(authorities);
        return authentication != null
                && getAuthorities(authentication).anyMatch(required::contains);
    }

    private @Nullable String extractPrincipal(@Nullable Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return switch (authentication.getPrincipal()) {
            case null -> null;
            case Jwt jwt -> jwt.getSubject();
            case UserDetails ud -> ud.getUsername();
            case String username -> username;
            default -> null;
        };
    }

    private Stream<@Nullable String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    }

    public boolean isCurrentUserAdmin() {
        return hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);
    }
}
