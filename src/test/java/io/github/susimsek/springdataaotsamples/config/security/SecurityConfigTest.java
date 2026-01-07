package io.github.susimsek.springdataaotsamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

class SecurityConfigTest {

    @Test
    void shouldCreateBeansAndBuildFilterChain() {
        ApplicationProperties props = new ApplicationProperties();
        props.getSecurity().getJwt().setSecret("my-secret-key-1234567890");
        SecurityConfig config = new SecurityConfig(props);

        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(manager);

        assertThat(config.authenticationManager(authConfig)).isSameAs(manager);
        assertThat(config.passwordEncoder()).isNotNull();

        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        SecurityFilterChain chain = mock(SecurityFilterChain.class);
        doReturn(chain).when(http).build();

        SecurityFilterChain result = config.securityFilterChain(http);
        assertThat(result).isSameAs(chain);
        verify(http).build();
    }
}
