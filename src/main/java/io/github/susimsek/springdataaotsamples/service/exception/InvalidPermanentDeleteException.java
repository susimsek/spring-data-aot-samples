package io.github.susimsek.springdataaotsamples.service.exception;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import java.util.Set;

@NullMarked
public class InvalidPermanentDeleteException extends ApiException {

    public InvalidPermanentDeleteException(Long id) {
        super(HttpStatus.CONFLICT, "Permanent delete not allowed",
                "Note " + id + " must be in trash before deleting permanently", id);
    }

    public InvalidPermanentDeleteException(Set<Long> ids) {
        super(HttpStatus.CONFLICT, "Permanent delete not allowed",
                "Notes must be in trash before deleting permanently: " + ids, ids.toArray());
    }
}
