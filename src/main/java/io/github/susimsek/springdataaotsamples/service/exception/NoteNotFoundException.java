package io.github.susimsek.springdataaotsamples.service.exception;

import org.springframework.http.HttpStatus;

public class NoteNotFoundException extends ApiException {

    public NoteNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Note not found", "Note not found with id: " + id, id);
    }
}
