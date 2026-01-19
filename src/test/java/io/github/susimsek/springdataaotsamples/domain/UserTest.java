package io.github.susimsek.springdataaotsamples.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void normalizeShouldTrimAndLowercaseUsernameAndEmail() {
        User user = new User();
        user.setUsername("  Alice  ");
        user.setEmail("  ALICE@EXAMPLE.COM  ");
        user.setPassword("secret");

        user.normalize();

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }
}
