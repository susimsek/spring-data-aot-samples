package io.github.susimsek.springdataaotsamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class HashingUtilsTest {

    @Test
    void sha256HexShouldReturnDeterministicHash() {
        String hash1 = HashingUtils.sha256Hex("secret");
        String hash2 = HashingUtils.sha256Hex("secret");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sha256HexShouldWrapCheckedExceptions() {
        try (MockedStatic<MessageDigest> digest = Mockito.mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new java.security.NoSuchAlgorithmException("missing"));

            assertThatThrownBy(() -> HashingUtils.sha256Hex("secret"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to hash token");
        }
    }
}
