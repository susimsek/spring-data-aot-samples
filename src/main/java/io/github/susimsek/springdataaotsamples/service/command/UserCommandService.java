package io.github.susimsek.springdataaotsamples.service.command;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.AuthorityRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
import io.github.susimsek.springdataaotsamples.service.dto.RegisterRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegistrationDTO;
import io.github.susimsek.springdataaotsamples.service.exception.EmailAlreadyExistsException;
import io.github.susimsek.springdataaotsamples.service.exception.UsernameAlreadyExistsException;
import io.github.susimsek.springdataaotsamples.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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
        user.getAuthorities()
                .add(
                        authorityRepository
                                .findByName(AuthoritiesConstants.USER)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Authority not found: "
                                                                + AuthoritiesConstants.USER)));
        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            String persistedUsername = user.getUsername();
            String persistedEmail = user.getEmail();
            if (userRepository.existsByUsername(persistedUsername)) {
                throw new UsernameAlreadyExistsException(persistedUsername);
            }
            if (userRepository.existsByEmail(persistedEmail)) {
                throw new EmailAlreadyExistsException(persistedEmail);
            }
            throw ex;
        }
        evictUserCaches();
        return userMapper.toRegistrationDto(saved);
    }

    private void evictUserCaches() {
        cacheProvider.clearCaches(User.class.getName(), UserRepository.USER_BY_USERNAME_CACHE);
    }
}
