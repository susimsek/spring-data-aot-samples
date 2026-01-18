package io.github.susimsek.springdataaotsamples.service.command;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.AuthorityRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
import io.github.susimsek.springdataaotsamples.service.dto.ChangePasswordRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegisterRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegistrationDTO;
import io.github.susimsek.springdataaotsamples.service.exception.EmailAlreadyExistsException;
import io.github.susimsek.springdataaotsamples.service.exception.InvalidPasswordException;
import io.github.susimsek.springdataaotsamples.service.exception.UsernameAlreadyExistsException;
import io.github.susimsek.springdataaotsamples.service.mapper.UserMapper;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheProvider cacheProvider;
    private final UserMapper userMapper;

    @Transactional
    public RegistrationDTO register(RegisterRequest request) {
        String username = request.username();
        String email = request.email();
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = userMapper.toEntity(request);
        user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
        user.setEnabled(true);
        Authority authority =
                authorityRepository
                        .findByName(AuthoritiesConstants.USER)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Authority not found: "
                                                        + AuthoritiesConstants.USER));
        user.getAuthorities().add(authority);
        User saved = userRepository.save(user);
        evictUserCaches(saved);
        return userMapper.toRegistrationDto(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user =
                userRepository
                        .findOneWithAuthoritiesById(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException(
                    "problemDetail.invalidPassword.currentPassword",
                    "Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidPasswordException(
                    "problemDetail.invalidPassword.samePassword",
                    "New password must be different from current password.");
        }
        user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.newPassword())));
        userRepository.save(user);
        evictUserCaches(user);
    }

    private void evictUserCaches(User user) {
        cacheProvider.clearCache(User.class.getName(), user.getId());
        cacheProvider.clearCache(UserRepository.USER_BY_USERNAME_CACHE, user.getUsername());
        cacheProvider.clearCache(UserRepository.USER_BY_EMAIL_CACHE, user.getEmail());
    }
}
