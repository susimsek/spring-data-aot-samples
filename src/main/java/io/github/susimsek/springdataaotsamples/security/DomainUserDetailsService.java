package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        boolean isEmail = new EmailValidator().isValid(normalized, null);
        User user;
        if (isEmail) {
            user =
                    userRepository
                            .findOneWithAuthoritiesByEmail(normalized)
                            .orElseThrow(
                                    () ->
                                            new UsernameNotFoundException(
                                                    "User not found by email: " + username));
        } else {
            user =
                    userRepository
                            .findOneWithAuthoritiesByUsername(normalized)
                            .orElseThrow(
                                    () ->
                                            new UsernameNotFoundException(
                                                    "User not found by username: " + username));
        }

        return UserPrincipal.from(user);
    }
}
