package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomUtilsTest {

    @Test
    void hexTokenShouldReturnHexStringWithExpectedLength() {
        String token = RandomUtils.hexToken(4);
        assertThat(token).hasSize(8).matches("[0-9a-f]+");
    }

    @Test
    void hexRefreshTokenShouldUseConfiguredByteLength() {
        String token = RandomUtils.hexRefreshToken();
        assertThat(token).hasSize(RandomUtils.REFRESH_TOKEN_BYTE_LENGTH * 2).matches("[0-9a-f]+");
    }
}
