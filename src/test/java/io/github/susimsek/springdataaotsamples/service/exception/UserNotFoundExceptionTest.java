package io.github.susimsek.springdataaotsamples.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UserNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithUsername() {
        String username = "testuser";
        UserNotFoundException exception = new UserNotFoundException(username);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getBody().getTitle()).isEqualTo("User not found");
        assertThat(exception.getMessage()).isEqualTo("User not found with username: " + username);
        assertThat(exception.getDetailMessageArguments()).containsExactly(username);
    }

    @Test
    void shouldCreateExceptionWithId() {
        Long id = 123L;
        UserNotFoundException exception = new UserNotFoundException(id);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getBody().getTitle()).isEqualTo("User not found");
        assertThat(exception.getMessage()).isEqualTo("User not found with id: " + id);
        assertThat(exception.getDetailMessageArguments()).containsExactly(id);
    }

    @Test
    void shouldHandleNullUsername() {
        String username = null;
        UserNotFoundException exception = new UserNotFoundException(username);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getBody().getTitle()).isEqualTo("User not found");
        assertThat(exception.getMessage()).isEqualTo("User not found with username: null");
    }

    @Test
    void shouldHandleNullId() {
        Long id = null;
        UserNotFoundException exception = new UserNotFoundException(id);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getBody().getTitle()).isEqualTo("User not found");
        assertThat(exception.getMessage()).isEqualTo("User not found with id: null");
    }

    @Test
    void shouldHandleEmptyUsername() {
        String username = "";
        UserNotFoundException exception = new UserNotFoundException(username);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getBody().getTitle()).isEqualTo("User not found");
        assertThat(exception.getMessage()).isEqualTo("User not found with username: ");
        assertThat(exception.getDetailMessageArguments()).containsExactly(username);
    }

    @Test
    void shouldVerifyExceptionIsRuntimeException() {
        UserNotFoundException exception = new UserNotFoundException("testuser");
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isInstanceOf(ApiException.class);
    }

    @Test
    void shouldVerifyProblemDetailProperties() {
        String username = "alice";
        UserNotFoundException exception = new UserNotFoundException(username);

        assertThat(exception.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exception.getBody().getDetail()).isEqualTo("User not found with username: " + username);
        assertThat(exception.getHeaders()).isNotNull();
    }

    @Test
    void shouldVerifyProblemDetailPropertiesWithId() {
        Long id = 999L;
        UserNotFoundException exception = new UserNotFoundException(id);

        assertThat(exception.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(exception.getBody().getDetail()).isEqualTo("User not found with id: " + id);
        assertThat(exception.getHeaders()).isNotNull();
    }
}
