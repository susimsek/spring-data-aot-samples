package io.github.susimsek.springdataaotsamples.service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InvalidPermanentDeleteExceptionTest {

    @Test
    void shouldFormatSingleIdMessage() {
        InvalidPermanentDeleteException ex = new InvalidPermanentDeleteException(5L);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getBody().getTitle()).isEqualTo("Permanent delete not allowed");
        assertThat(ex.getMessage()).contains("Note 5 must be in trash before deleting permanently");
        assertThat(ex.getDetailMessageArguments()).containsExactly(5L);
    }

    @Test
    void shouldFormatMultipleIdsMessage() {
        Set<Long> ids = Set.of(1L, 2L, 3L);
        InvalidPermanentDeleteException ex = new InvalidPermanentDeleteException(ids);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getBody().getTitle()).isEqualTo("Permanent delete not allowed");
        assertThat(ex.getMessage())
                .contains("Notes must be in trash before deleting permanently: " + ids);
        assertThat(ex.getDetailMessageArguments()).containsExactlyInAnyOrder(ids.toArray());
    }
}
