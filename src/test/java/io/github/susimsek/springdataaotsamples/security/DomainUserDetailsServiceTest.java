package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DomainUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private DomainUserDetailsService domainUserDetailsService;

    @Test
    void loadUserByUsernameShouldMapUserToPrincipal() {
        User user = sampleUser(true, Set.of("ROLE_USER", "ROLE_ADMIN"));
        when(userRepository.findOneWithAuthoritiesByUsername("alice"))
                .thenReturn(Optional.of(user));

        UserDetails details = domainUserDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("secret");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        verify(userRepository).findOneWithAuthoritiesByUsername("alice");
    }

    @Test
    void loadUserByUsernameShouldThrowWhenUserMissing() {
        when(userRepository.findOneWithAuthoritiesByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> domainUserDetailsService.loadUserByUsername("missing"));

        verify(userRepository).findOneWithAuthoritiesByUsername("missing");
    }

    private static User sampleUser(boolean enabled, Set<String> authorities) {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("secret");
        user.setEnabled(enabled);
        Set<Authority> userAuthorities = new HashSet<>();
        long id = 1;
        for (String name : authorities) {
            userAuthorities.add(new Authority(id++, name));
        }
        user.setAuthorities(userAuthorities);
        return user;
    }
}
