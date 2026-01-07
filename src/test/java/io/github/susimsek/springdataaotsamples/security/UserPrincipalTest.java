package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void fromShouldMapUserToPrincipal() {
        Authority auth = new Authority();
        auth.setName("ROLE_USER");
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("secret");
        user.setEnabled(true);
        user.setAuthorities(Set.of(auth));

        UserPrincipal principal = UserPrincipal.from(user);

        assertThat(principal.id()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getPassword()).isEqualTo("secret");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }
}
