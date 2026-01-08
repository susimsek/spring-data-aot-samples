package io.github.susimsek.springdataaotsamples.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

class ViolationTest {

    @Test
    void fromShouldMapFieldError() {
        FieldError error =
                new FieldError(
                        "UserDTO",
                        "username",
                        "alice",
                        false,
                        new String[] {"Size"},
                        null,
                        "too short");

        Violation violation = Violation.from(error);

        assertThat(violation)
                .isEqualTo(new Violation("Size", "User", "username", "alice", "too short"));
    }

    @Test
    void fromShouldMapObjectError() {
        ObjectError error =
                new ObjectError("LoginDTO", new String[] {"NotBlank"}, null, "refresh required");

        Violation violation = Violation.from(error);

        assertThat(violation)
                .isEqualTo(
                        new Violation("NotBlank", "Login", "LoginDTO", null, "refresh required"));
    }
}
