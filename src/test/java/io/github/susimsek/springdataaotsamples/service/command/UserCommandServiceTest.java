package io.github.susimsek.springdataaotsamples.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.cache.CacheProvider;
import io.github.susimsek.springdataaotsamples.domain.Authority;
import io.github.susimsek.springdataaotsamples.domain.User;
import io.github.susimsek.springdataaotsamples.repository.AuthorityRepository;
import io.github.susimsek.springdataaotsamples.repository.UserRepository;
import io.github.susimsek.springdataaotsamples.security.AuthoritiesConstants;
import io.github.susimsek.springdataaotsamples.service.dto.RegisterRequest;
import io.github.susimsek.springdataaotsamples.service.dto.RegistrationDTO;
import io.github.susimsek.springdataaotsamples.service.exception.UsernameAlreadyExistsException;
import io.github.susimsek.springdataaotsamples.service.mapper.UserMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthorityRepository authorityRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CacheProvider cacheProvider;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserCommandService userCommandService;

    @Test
    void registerShouldCreateEnabledUserWithRoleUser() {
        RegisterRequest request =
                new RegisterRequest("newuser", "newuser@example.com", "change-me");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("change-me")).thenReturn("{bcrypt}hash");
        Authority roleUser = new Authority(2L, AuthoritiesConstants.USER);
        when(authorityRepository.findByName(AuthoritiesConstants.USER))
                .thenReturn(Optional.of(roleUser));
        when(userMapper.toEntity(any(RegisterRequest.class)))
                .thenAnswer(
                        invocation -> {
                            RegisterRequest req = invocation.getArgument(0);
                            User user = new User();
                            user.setUsername(req.username());
                            user.setEmail(req.email());
                            return user;
                        });
        when(userMapper.toRegistrationDto(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            return new RegistrationDTO(user.getId(), user.getUsername());
                        });
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(10L);
                            return user;
                        });

        RegistrationDTO created = userCommandService.register(request);

        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.username()).isEqualTo("newuser");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("newuser@example.com");
        verify(cacheProvider)
                .clearCaches(User.class.getName(), UserRepository.USER_BY_USERNAME_CACHE);
    }

    @Test
    void registerShouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("admin", "admin@example.com", "change-me");
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userCommandService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }
}
