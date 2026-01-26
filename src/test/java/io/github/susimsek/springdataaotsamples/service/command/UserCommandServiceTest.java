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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(userRepository.save(any(User.class)))
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
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("newuser@example.com");
        verify(cacheProvider).clearCache(User.class.getName(), 10L);
        verify(cacheProvider).clearCache(UserRepository.USER_BY_USERNAME_CACHE, "newuser");
        verify(cacheProvider).clearCache(UserRepository.USER_BY_EMAIL_CACHE, "newuser@example.com");
    }

    @Test
    void registerShouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("admin", "admin@example.com", "change-me");
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userCommandService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void registerShouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("newuser", "admin@example.com", "change-me");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userCommandService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void changePasswordShouldUpdatePasswordWhenCurrentPasswordIsCorrect() {
        Long userId = 1L;
        ChangePasswordRequest request =
                new ChangePasswordRequest("oldPassword123", "newPassword456");
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword("{bcrypt}oldHashedPassword");

        when(userRepository.findOneWithAuthoritiesById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword123", "{bcrypt}oldHashedPassword"))
                .thenReturn(true);
        when(passwordEncoder.matches("newPassword456", "{bcrypt}oldHashedPassword"))
                .thenReturn(false);
        when(passwordEncoder.encode("newPassword456")).thenReturn("{bcrypt}newHashedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userCommandService.changePassword(userId, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("{bcrypt}newHashedPassword");
        verify(cacheProvider).clearCache(User.class.getName(), userId);
        verify(cacheProvider).clearCache(UserRepository.USER_BY_USERNAME_CACHE, "testuser");
        verify(cacheProvider)
                .clearCache(UserRepository.USER_BY_EMAIL_CACHE, "testuser@example.com");
    }

    @Test
    void changePasswordShouldThrowWhenUserNotFound() {
        Long userId = 999L;
        ChangePasswordRequest request =
                new ChangePasswordRequest("oldPassword123", "newPassword456");

        when(userRepository.findOneWithAuthoritiesById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCommandService.changePassword(userId, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void changePasswordShouldThrowWhenCurrentPasswordIsIncorrect() {
        Long userId = 1L;
        ChangePasswordRequest request =
                new ChangePasswordRequest("wrongPassword", "newPassword456");
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword("{bcrypt}oldHashedPassword");

        when(userRepository.findOneWithAuthoritiesById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "{bcrypt}oldHashedPassword"))
                .thenReturn(false);

        assertThatThrownBy(() -> userCommandService.changePassword(userId, request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasFieldOrPropertyWithValue(
                        "detailMessageCode", "problemDetail.invalidPassword.currentPassword")
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePasswordShouldThrowWhenNewPasswordIsSameAsCurrentPassword() {
        Long userId = 1L;
        ChangePasswordRequest request =
                new ChangePasswordRequest("samePassword123", "samePassword123");
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword("{bcrypt}hashedPassword");

        when(userRepository.findOneWithAuthoritiesById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("samePassword123", "{bcrypt}hashedPassword")).thenReturn(true);

        assertThatThrownBy(() -> userCommandService.changePassword(userId, request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasFieldOrPropertyWithValue(
                        "detailMessageCode", "problemDetail.invalidPassword.samePassword")
                .hasMessageContaining("New password must be different from current password");
    }
}
