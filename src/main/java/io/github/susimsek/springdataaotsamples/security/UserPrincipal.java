package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public record UserPrincipal(
        Long id,
        String username,
        @Nullable String password,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities)
        implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public static UserPrincipal from(User user) {
        Set<GrantedAuthority> granted =
                user.getAuthorities().stream()
                        .map(Authority::getName)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());
        return new UserPrincipal(
                user.getId(), user.getUsername(), user.getPassword(), user.isEnabled(), granted);
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
