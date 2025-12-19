package io.github.susimsek.springdataaotsamples.security;

import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    var user =
        userRepository
            .findOneWithAuthoritiesByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return UserPrincipal.from(user);
  }
}
